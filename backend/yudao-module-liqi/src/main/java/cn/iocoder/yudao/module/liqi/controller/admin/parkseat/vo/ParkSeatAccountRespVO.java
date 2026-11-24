package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 园区可指派账号 Response VO")
@Data
public class ParkSeatAccountRespVO {

    private Long userId;
    private String userName;
    private String phone;
    private String company;
    private Long parkId;
    private String parkName;
    @Schema(description = "会员状态：NONE / ACTIVE / EXPIRED")
    private String memberStatus;
    private String memberPlanName;
    private LocalDateTime memberExpireTime;
    private BigDecimal memberPrice;
    @Schema(description = "指派后企业归属是否变化")
    private Boolean companyChanged;
}
