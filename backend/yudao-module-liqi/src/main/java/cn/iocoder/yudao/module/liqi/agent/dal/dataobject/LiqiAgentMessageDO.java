package cn.iocoder.yudao.module.liqi.agent.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 力企 - 智能体广场 · 消息 DO
 */
@TableName("liqi_agent_message")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiAgentMessageDO extends TenantBaseDO {

    /** 编号 */
    @TableId
    private Long id;
    /** 会话编号 */
    private Long conversationId;
    /** 角色：user / assistant / tool */
    private String role;
    /** 消息内容 */
    private String content;
    /** 工具调用留痕（证据：入参 + 返回，JSON 文本） */
    private String toolCalls;

}
