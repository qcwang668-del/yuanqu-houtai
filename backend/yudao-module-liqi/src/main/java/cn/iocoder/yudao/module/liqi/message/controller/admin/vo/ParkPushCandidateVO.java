package cn.iocoder.yudao.module.liqi.message.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 园区政策推送 - 候选企业 VO（园区运营预览名单用）
 */
@Data
@Schema(description = "管理后台 - 园区政策推送候选企业 VO")
public class ParkPushCandidateVO {

    @Schema(description = "会员用户 ID（勾选推送用；未注册绑定小程序时为空，不可推送）")
    private Long userId;

    @Schema(description = "企业名称")
    private String enterpriseName;

    @Schema(description = "法定代表人")
    private String legalPerson;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "所属地区")
    private String region;

    @Schema(description = "所属园区")
    private String parkName;

    @Schema(description = "是否画像匹配该政策")
    private Boolean matched;

    @Schema(description = "匹配理由（地区/行业/资质命中项）")
    private List<String> reasons;

    @Schema(description = "是否已推送过该政策")
    private Boolean pushed;

    // ---------------- 客户对象画像圈选：企业画像字段（供运营核对） ----------------

    @Schema(description = "参保人数")
    private Integer insuredCount;

    @Schema(description = "是否有融资记录")
    private Boolean financed;

    @Schema(description = "成立日期")
    private LocalDate establishDate;

    @Schema(description = "一级行业名")
    private String industryLv1Name;

    @Schema(description = "二级行业名")
    private String industryLv2Name;

    @Schema(description = "实缴资本（万元，取 reg_capital_amount）")
    private BigDecimal actualCapital;

    /**
     * 是否可推送：园区客户管理中的企业需已在小程序注册并绑定，才有 userId 可接收站内消息。
     * false 时前端置灰不可勾选，并展示 {@link #unpushableReason}。
     */
    @Schema(description = "是否可推送（未注册绑定小程序的客户不可推送）")
    private Boolean pushable;

    @Schema(description = "不可推送原因")
    private String unpushableReason;

}
