package cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 力企 - 园区席位增购/续费记录 */
@TableName("liqi_park_seat_order")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkSeatOrderDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long parkId;
    private String orderNo;
    private String orderType;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime payTime;
    private String remark;
}
