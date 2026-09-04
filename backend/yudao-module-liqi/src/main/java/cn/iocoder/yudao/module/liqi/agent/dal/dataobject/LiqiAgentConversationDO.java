package cn.iocoder.yudao.module.liqi.agent.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 力企 - 智能体广场 · 会话 DO
 */
@TableName("liqi_agent_conversation")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiAgentConversationDO extends TenantBaseDO {

    /** 编号 */
    @TableId
    private Long id;
    /** 智能体编号 */
    private Long agentId;
    /** 用户编号（管理后台登录用户） */
    private Long userId;
    /** 会话标题 */
    private String title;

}
