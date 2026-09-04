package cn.iocoder.yudao.module.liqi.agent.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.liqi.agent.config.LiqiAgentProperties;
import cn.iocoder.yudao.module.liqi.agent.dal.dataobject.LiqiAgentConversationDO;
import cn.iocoder.yudao.module.liqi.agent.dal.dataobject.LiqiAgentDO;
import cn.iocoder.yudao.module.liqi.agent.dal.dataobject.LiqiAgentMessageDO;
import cn.iocoder.yudao.module.liqi.agent.dal.mysql.LiqiAgentConversationMapper;
import cn.iocoder.yudao.module.liqi.agent.dal.mysql.LiqiAgentMapper;
import cn.iocoder.yudao.module.liqi.agent.dal.mysql.LiqiAgentMessageMapper;
import cn.iocoder.yudao.module.liqi.agent.llm.ArkClient;
import cn.iocoder.yudao.module.liqi.agent.mcp.McpClient;
import cn.iocoder.yudao.module.liqi.agent.mcp.McpSession;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.liqi.enums.ErrorCodeConstants.AGENT_CONVERSATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.liqi.enums.ErrorCodeConstants.AGENT_NOT_EXISTS;

/**
 * 力企 - 智能体广场 Service 实现（方案①：自建轻量对话 + MCP，纯 JDK8）
 */
@Slf4j
@Service
public class LiqiAgentServiceImpl implements LiqiAgentService {

    /** function-calling 最大轮次，防止死循环 */
    private static final int MAX_TURNS = 5;

    @Resource
    private LiqiAgentMapper agentMapper;
    @Resource
    private LiqiAgentConversationMapper conversationMapper;
    @Resource
    private LiqiAgentMessageMapper messageMapper;
    @Resource
    private ArkClient arkClient;
    @Resource
    private McpClient mcpClient;
    @Resource
    private LiqiAgentProperties properties;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public List<LiqiAgentDO> getEnabledAgents() {
        return agentMapper.selectEnabledList();
    }

    @Override
    public Long createConversation(Long agentId, Long userId) {
        LiqiAgentDO agent = agentMapper.selectById(agentId);
        if (agent == null || (agent.getStatus() != null && agent.getStatus() != 0)) {
            throw exception(AGENT_NOT_EXISTS);
        }
        LiqiAgentConversationDO conv = LiqiAgentConversationDO.builder()
                .agentId(agentId).userId(userId).title("新会话").build();
        conversationMapper.insert(conv);
        return conv.getId();
    }

    @Override
    public List<LiqiAgentConversationDO> getConversations(Long agentId, Long userId) {
        return conversationMapper.selectListByAgentAndUser(agentId, userId);
    }

    @Override
    public List<LiqiAgentMessageDO> getMessages(Long conversationId) {
        return messageMapper.selectListByConversationId(conversationId);
    }

    @Override
    public SseEmitter sendStream(Long conversationId, String content, Long userId) {
        LiqiAgentConversationDO conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw exception(AGENT_CONVERSATION_NOT_EXISTS);
        }
        LiqiAgentDO agent = agentMapper.selectById(conv.getAgentId());
        if (agent == null) {
            throw exception(AGENT_NOT_EXISTS);
        }
        // 持久化用户消息
        messageMapper.insert(LiqiAgentMessageDO.builder()
                .conversationId(conversationId).role("user").content(content).build());
        // 首条消息作为标题
        if (StrUtil.isBlank(conv.getTitle()) || "新会话".equals(conv.getTitle())) {
            LiqiAgentConversationDO update = new LiqiAgentConversationDO();
            update.setId(conversationId);
            update.setTitle(StrUtil.sub(content, 0, 20));
            conversationMapper.updateById(update);
        }

        SseEmitter emitter = new SseEmitter(300000L);
        // 捕获租户上下文，异步线程内恢复（否则多租户过滤拿不到 tenant_id）
        Long tenantId = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getTenantId();
        executor.submit(() -> {
            try {
                TenantUtils.execute(tenantId, () -> orchestrate(emitter, agent, conversationId));
            } catch (Exception e) {
                log.error("[智能体] 编排线程异常 convId={}", conversationId, e);
                sendError(emitter, e.getMessage());
            }
        });
        return emitter;
    }

    /** function-calling 编排主流程（运行在异步线程，已恢复租户上下文）。 */
    private void orchestrate(SseEmitter emitter, LiqiAgentDO agent, Long conversationId) {
        // 1. 组装 messages：system + 历史（user/assistant）
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system")
                .set("content", StrUtil.nullToEmpty(agent.getSystemPrompt())));
        for (LiqiAgentMessageDO m : messageMapper.selectListByConversationId(conversationId)) {
            if ("user".equals(m.getRole()) || "assistant".equals(m.getRole())) {
                messages.add(new JSONObject().set("role", m.getRole())
                        .set("content", StrUtil.nullToEmpty(m.getContent())));
            }
        }

        // 2. MCP 工具（仅 mcp_enabled 且握手成功时启用；失败优雅降级为无工具）
        JSONArray tools = null;
        McpSession mcp = null;
        if (Boolean.TRUE.equals(agent.getMcpEnabled()) && StrUtil.isNotBlank(agent.getMcpUrl())) {
            try {
                mcp = mcpClient.openSession(agent.getMcpUrl(), properties.getMcpToken());
                mcp.connect();
                tools = mcpClient.toOpenAiTools(mcp.listTools());
                log.info("[智能体] MCP 工具加载成功 count={}", tools.size());
            } catch (Exception e) {
                log.warn("[智能体] MCP 连接失败，降级为无工具对话：{}", e.getMessage());
                mcp = null;
                tools = null;
            }
        }

        // 3. function-calling 循环
        JSONArray evidence = new JSONArray();
        String finalContent = "";
        for (int turn = 0; turn < MAX_TURNS; turn++) {
            ArkClient.ChatResult r = arkClient.chat(
                    properties.getArkUrl(), properties.getArkKey(), agent.getModel(),
                    messages, tools, delta -> sendDelta(emitter, delta));

            if (r.hasToolCalls() && mcp != null) {
                // 追加 assistant（带 tool_calls）
                JSONArray tcArr = new JSONArray();
                for (ArkClient.ToolCall tc : r.getToolCalls()) {
                    tcArr.add(new JSONObject().set("id", tc.getId()).set("type", "function")
                            .set("function", new JSONObject().set("name", tc.getName())
                                    .set("arguments", tc.getArguments().toString())));
                }
                messages.add(new JSONObject().set("role", "assistant")
                        .set("content", StrUtil.nullToEmpty(r.getContent())).set("tool_calls", tcArr));
                // 逐个执行工具
                for (ArkClient.ToolCall tc : r.getToolCalls()) {
                    String argStr = tc.getArguments().toString();
                    Map<String, Object> argsMap = new HashMap<>();
                    if (JSONUtil.isTypeJSONObject(argStr)) {
                        argsMap = JSONUtil.parseObj(argStr);
                    }
                    String toolResult;
                    try {
                        log.info("[智能体] 调用 MCP 工具 name={} args={}", tc.getName(), argStr);
                        toolResult = mcp.callTool(tc.getName(), argsMap);
                    } catch (Exception e) {
                        toolResult = "数据源暂时不可用（工具 " + tc.getName() + " 调用失败）：" + e.getMessage();
                        log.warn("[智能体] MCP 工具调用失败 name={}：{}", tc.getName(), e.getMessage());
                    }
                    evidence.add(new JSONObject().set("tool", tc.getName())
                            .set("arguments", argStr).set("result", StrUtil.sub(toolResult, 0, 4000)));
                    messages.add(new JSONObject().set("role", "tool")
                            .set("tool_call_id", tc.getId()).set("content", toolResult));
                }
                continue; // 带工具结果再次请求模型
            }

            if (r.hasToolCalls()) {
                // 模型想调用工具但 MCP 不可用：优雅兜底
                finalContent = StrUtil.isNotBlank(r.getContent()) ? r.getContent()
                        : "抱歉，当前数据源暂时不可用，无法查询实时数据，请稍后再试。";
                if (StrUtil.isBlank(r.getContent())) {
                    sendDelta(emitter, finalContent);
                }
                break;
            }

            // 正常最终答复
            finalContent = r.getContent();
            break;
        }

        // 4. 持久化 assistant 消息（含工具证据留痕）
        messageMapper.insert(LiqiAgentMessageDO.builder()
                .conversationId(conversationId).role("assistant")
                .content(finalContent)
                .toolCalls(evidence.isEmpty() ? null : evidence.toString())
                .build());

        // 5. 完成事件
        sendDone(emitter);
        emitter.complete();
    }

    // ================= SSE 下发 =================

    private void sendDelta(SseEmitter emitter, String text) {
        JSONObject payload = new JSONObject().set("code", 0)
                .set("data", new JSONObject().set("receive", new JSONObject().set("content", text)));
        try {
            emitter.send(SseEmitter.event().data(payload.toString()));
        } catch (Exception e) {
            throw new RuntimeException("SSE 下发失败：" + e.getMessage(), e);
        }
    }

    private void sendDone(SseEmitter emitter) {
        JSONObject payload = new JSONObject().set("code", 0)
                .set("data", new JSONObject().set("done", true)
                        .set("receive", new JSONObject().set("content", "")));
        try {
            emitter.send(SseEmitter.event().data(payload.toString()));
        } catch (Exception ignored) {
        }
    }

    private void sendError(SseEmitter emitter, String msg) {
        JSONObject payload = new JSONObject().set("code", 1).set("msg", StrUtil.nullToEmpty(msg));
        try {
            emitter.send(SseEmitter.event().data(payload.toString()));
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

}
