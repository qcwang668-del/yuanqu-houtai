package cn.iocoder.yudao.module.liqi.agent.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.liqi.agent.controller.admin.vo.*;
import cn.iocoder.yudao.module.liqi.agent.service.LiqiAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static java.util.Collections.singletonMap;

/**
 * 管理后台 - 智能体广场（方案①：自建轻量对话 + MCP）
 */
@Tag(name = "管理后台 - 智能体广场")
@RestController
@RequestMapping("/liqi/agent")
@Validated
public class LiqiAgentController {

    @Resource
    private LiqiAgentService agentService;

    @GetMapping("/list")
    @Operation(summary = "获得智能体列表")
    @PreAuthorize("@ss.hasPermission('liqi:agent:query')")
    public CommonResult<List<AgentRespVO>> getAgentList() {
        return success(BeanUtils.toBean(agentService.getEnabledAgents(), AgentRespVO.class));
    }

    @PostMapping("/conversation/create")
    @Operation(summary = "创建会话")
    @PreAuthorize("@ss.hasPermission('liqi:agent:query')")
    public CommonResult<Object> createConversation(@Valid @RequestBody ConversationCreateReqVO reqVO) {
        Long conversationId = agentService.createConversation(reqVO.getAgentId(),
                SecurityFrameworkUtils.getLoginUserId());
        return success(singletonMap("conversationId", conversationId));
    }

    @GetMapping("/conversation/list")
    @Operation(summary = "获得会话列表")
    @Parameter(name = "agentId", description = "智能体编号")
    @PreAuthorize("@ss.hasPermission('liqi:agent:query')")
    public CommonResult<List<ConversationRespVO>> getConversationList(@RequestParam(value = "agentId", required = false) Long agentId) {
        return success(BeanUtils.toBean(
                agentService.getConversations(agentId, SecurityFrameworkUtils.getLoginUserId()),
                ConversationRespVO.class));
    }

    @GetMapping("/message/list")
    @Operation(summary = "获得消息列表")
    @Parameter(name = "conversationId", description = "会话编号", required = true)
    @PreAuthorize("@ss.hasPermission('liqi:agent:query')")
    public CommonResult<List<MessageRespVO>> getMessageList(@RequestParam("conversationId") Long conversationId) {
        return success(BeanUtils.toBean(agentService.getMessages(conversationId), MessageRespVO.class));
    }

    @PostMapping("/message/send-stream")
    @Operation(summary = "发送消息（流式）")
    @PreAuthorize("@ss.hasPermission('liqi:agent:query')")
    public SseEmitter sendMessageStream(@Valid @RequestBody MessageSendReqVO reqVO) {
        return agentService.sendStream(reqVO.getConversationId(), reqVO.getContent(),
                SecurityFrameworkUtils.getLoginUserId());
    }

}
