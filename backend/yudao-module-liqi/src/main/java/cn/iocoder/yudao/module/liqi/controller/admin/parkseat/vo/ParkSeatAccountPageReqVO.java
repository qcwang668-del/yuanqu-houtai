package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 园区账号分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ParkSeatAccountPageReqVO extends PageParam {

    @Schema(description = "园区编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long parkId;
    @Schema(description = "用户名 / 手机号 / 企业")
    private String keyword;
}
