package cn.iocoder.yudao.module.liqi.agent.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 力企 - 智能体广场 · 智能体定义 DO
 *
 * 方案①：自建轻量对话 + MCP（不依赖 spring-ai，纯 JDK8）
 */
@TableName("liqi_agent")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiAgentDO extends TenantBaseDO {

    /** 编号 */
    @TableId
    private Long id;
    /** 智能体名称 */
    private String name;
    /** 头像 URL */
    private String avatar;
    /** 描述 */
    private String description;
    /** 系统提示词 */
    private String systemPrompt;
    /** 大模型供应商，如 volcengine（火山方舟） */
    private String provider;
    /** 模型标识，如 ark-code-latest */
    private String model;
    /** 是否启用 MCP 工具 */
    private Boolean mcpEnabled;
    /** MCP 服务地址 */
    private String mcpUrl;
    /** 排序 */
    private Integer sort;
    /** 状态：0=开启，1=关闭 */
    private Integer status;

}
