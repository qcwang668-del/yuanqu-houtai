package cn.iocoder.yudao.module.liqi.dal.dataobject.park;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 力企 - 园区 DO
 */
@TableName("liqi_park")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiParkDO extends TenantBaseDO {

    /** 编号 */
    @TableId
    private Long id;
    /** 园区名称 */
    private String parkName;
    /** 地址匹配关键词（用于 LIKE 会员注册地址） */
    private String addressKeyword;
    /** 城市 */
    private String city;
    /** 状态：0=启用，1=停用 */
    private Integer status;

}
