package cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 力企 - 企业库 · 联系方式 DO
 */
@TableName("liqi_enterprise_contact")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiEnterpriseContactDO extends TenantBaseDO {

    @TableId
    private Long id;
    /** 企业编号 */
    private Long enterpriseId;
    /** 联系方式（号码/邮箱） */
    private String contact;
    /** 联系人姓名 */
    private String name;
    /** 类型：mobile 手机 / tel 座机 / email 邮箱 */
    private String contactType;
    /** 标签（; 分隔，如 疑似法人;疑似高管） */
    private String tags;
    /** 星级 1-5 */
    private Integer star;
    /** 来源平台 */
    private String platform;

}
