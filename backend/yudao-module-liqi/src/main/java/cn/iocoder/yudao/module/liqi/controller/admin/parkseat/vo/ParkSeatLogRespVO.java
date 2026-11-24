package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 园区席位指派记录 Response VO")
@Data
public class ParkSeatLogRespVO {

    private Long id;
    private Long batchId;
    private Long userId;
    private String userName;
    private String phone;
    private String company;
    private String planName;
    private String action;
    private String result;
    private String remark;
    private String operator;
    private LocalDateTime createTime;
}
