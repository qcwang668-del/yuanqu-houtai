package cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/** 力企 - 会员套餐定义 */
@TableName("liqi_member_plan")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPlanDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String planCode;
    private String planName;
    private BigDecimal price;
    private Integer durationDays;
    private String benefits;
    private Integer sort;
    private Integer status;
}
