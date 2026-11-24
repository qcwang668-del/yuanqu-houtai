package cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/** 力企 - 园区席位授权批次 */
@TableName("liqi_park_seat_batch")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkSeatBatchDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long parkId;
    private String batchNo;
    private String source;
    private Integer qty;
    private Integer usedQty;
    private LocalDate startDate;
    private LocalDate expireDate;
    private Integer status;
}
