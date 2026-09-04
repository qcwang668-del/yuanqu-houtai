package cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 力企 - 企业库 · 知识产权 DO
 */
@TableName("liqi_enterprise_ip")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiEnterpriseIpDO extends TenantBaseDO {

    @TableId
    private Long id;
    /** 企业编号 */
    private Long enterpriseId;
    /** 类型：patent 专利 / trademark 商标 / copyright 著作权 */
    private String ipType;
    /** 名称 */
    private String name;
    /** 分类 */
    private String category;
    /** 申请日期 */
    private String applyDate;
    /** 状态 */
    private String status;

}
