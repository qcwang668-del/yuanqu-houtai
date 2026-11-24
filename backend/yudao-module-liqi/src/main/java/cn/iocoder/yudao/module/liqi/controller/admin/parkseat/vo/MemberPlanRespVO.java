package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 会员套餐 Response VO")
@Data
public class MemberPlanRespVO {

    private String planCode;
    private String planName;
    private BigDecimal price;
    private Integer durationDays;
    private String benefits;
}
