package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 园区增购/续费 Request VO")
@Data
public class ParkSeatOrderCreateReqVO {

    @NotNull(message = "园区编号不能为空")
    private Long parkId;

    @Schema(description = "PURCHASE=增购，RENEW=续费", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    @NotNull(message = "席位数不能为空")
    @Min(value = 1, message = "席位数至少为 1")
    private Integer qty;

    @Schema(description = "合同单价，不填默认 800")
    private BigDecimal unitPrice;
}
