package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 园区席位总览 Response VO")
@Data
public class ParkSeatOverviewRespVO {

    private Long parkId;
    private String parkName;
    @Schema(description = "有效授权席位")
    private Integer totalQty;
    @Schema(description = "已指派（有效会员占用）")
    private Integer usedQty;
    @Schema(description = "剩余可指派")
    private Integer remainingQty;
    @Schema(description = "30 天内到期")
    private Integer expiringQty;
    @Schema(description = "企业归属变化")
    private Integer companyChangedQty;
    private Integer batchCount;
    private List<BatchVO> batches;

    @Schema(description = "授权批次")
    @Data
    public static class BatchVO {
        private Long id;
        private String batchNo;
        private String source;
        private Integer qty;
        private Integer usedQty;
        private Integer remainingQty;
        private LocalDate startDate;
        private LocalDate expireDate;
        private Integer status;
        private BigDecimal unitPrice;
    }
}
