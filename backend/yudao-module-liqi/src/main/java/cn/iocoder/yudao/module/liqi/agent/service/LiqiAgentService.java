package cn.iocoder.yudao.module.liqi.agent.service;

import cn.iocoder.yudao.module.liqi.agent.dal.dataobject.LiqiAgentConversationDO;
import cn.iocoder.yudao.module.liqi.agent.dal.dataobject.LiqiAgentDO;
import cn.iocoder.yudao.module.liqi.agent.dal.dataobject.LiqiAgentMessageDO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 力企 - 智能体广场 Service
 */
public interface LiqiAgentService {

    /** 已开启的智能体列表 */
    List<LiqiAgentDO> getEnabledAgents();

    /** 创建会话，返回会话编号 */
    Long createConversation(Long agentId, Long userId);

    /** 某智能体下当前用户的会话列表 */
    List<LiqiAgentConversationDO> getConversations(Long agentId, Long userId);

    /** 会话内消息列表 */
    List<LiqiAgentMessageDO> getMessages(Long conversationId);

    /** 发送消息并流式返回（function-calling 编排 + MCP） */
    SseEmitter sendStream(Long conversationId, String content, Long userId);

}
