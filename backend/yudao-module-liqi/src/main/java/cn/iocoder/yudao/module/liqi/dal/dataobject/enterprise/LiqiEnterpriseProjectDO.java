package cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 力企 - 企业库 · 项目申报 DO
 */
@TableName("liqi_enterprise_project")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiEnterpriseProjectDO extends TenantBaseDO {

    @TableId
    private Long id;
    /** 企业编号 */
    private Long enterpriseId;
    /** 项目名称 */
    private String title;
    /** 主管部门 */
    private String org;
    /** 地区 */
    private String region;
    /** 申报年度 */
    private String declareYear;
    /** 补贴金额（万元） */
    private String subsidy;

}
