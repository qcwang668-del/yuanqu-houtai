package cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 力企 - 企业库 · 经营风险 DO
 */
@TableName("liqi_enterprise_risk")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiEnterpriseRiskDO extends TenantBaseDO {

    @TableId
    private Long id;
    /** 企业编号 */
    private Long enterpriseId;
    /** 风险类型：judgment 裁判文书 / punish 行政处罚 / abnormal 经营异常 */
    private String riskType;
    /** 标题/案由 */
    private String title;
    /** 案号 */
    private String caseNo;
    /** 身份角色 */
    private String role;
    /** 处理机关 */
    private String org;
    /** 发生日期 */
    private String riskDate;

}
