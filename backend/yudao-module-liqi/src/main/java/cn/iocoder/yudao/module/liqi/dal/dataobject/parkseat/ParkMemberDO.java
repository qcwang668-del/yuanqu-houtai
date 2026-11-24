package cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 力企 - 园区会员权益 */
@TableName("liqi_park_member")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkMemberDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long parkId;
    private Long userId;
    private String phone;
    private String userName;
    private String company;
    private String planCode;
    private String planName;
    private BigDecimal price;
    private LocalDateTime startTime;
    private LocalDateTime expireTime;
    private String source;
    private Long batchId;
    private Integer status;
}
