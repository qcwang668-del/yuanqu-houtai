package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 园区会员指派 Request VO")
@Data
public class ParkSeatAssignReqVO {

    @Schema(description = "园区编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "园区编号不能为空")
    private Long parkId;

    @Schema(description = "平台账号编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账号编号不能为空")
    private Long userId;

    @Schema(description = "套餐编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "套餐编码不能为空")
    private String planCode;
}
