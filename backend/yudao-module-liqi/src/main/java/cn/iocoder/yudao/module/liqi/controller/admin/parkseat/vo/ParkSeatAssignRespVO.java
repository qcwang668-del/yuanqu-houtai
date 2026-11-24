package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 园区会员指派 Response VO")
@Data
public class ParkSeatAssignRespVO {

    private Long memberId;
    private String planName;
    private LocalDateTime startTime;
    private LocalDateTime expireTime;
    private Integer remainingQty;
}
