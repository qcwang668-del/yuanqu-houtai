package cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/** 力企 - 园区席位指派记录 */
@TableName("liqi_park_seat_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkSeatLogDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long parkId;
    private Long batchId;
    private Long userId;
    private String phone;
    private String userName;
    private String company;
    private String planCode;
    private String planName;
    private String action;
    private String result;
    private String remark;
    private String operator;
}
