package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 园区增购/续费记录 Response VO")
@Data
public class ParkSeatOrderRespVO {

    private Long id;
    private String orderNo;
    private String orderType;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime payTime;
    private String remark;
    private LocalDateTime createTime;
}
