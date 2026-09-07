package cn.iocoder.yudao.module.liqi.message.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 力企 - 园区发布项目「客户对象」企业画像筛选条件 VO。
 *
 * <p>一个类两用：既作为筛选接口的请求体，也作为 {@code liqi_policy.push_filter_conditions}
 * 的 JSON 快照结构，避免两处结构漂移。</p>
 *
 * <p>圈选范围固定为园区客户管理（liqi_member）中的企业，本 VO 只描述画像过滤条件。</p>
 */
@Schema(description = "管理后台 - 园区推送客户对象筛选条件 VO")
@Data
public class ParkPushFilterVO {

    @Schema(description = "参保人数下限", example = "50")
    private Integer insuredCountMin;

    @Schema(description = "参保人数上限", example = "200")
    private Integer insuredCountMax;

    @Schema(description = "是否有融资：true 有融资记录 / false 无融资记录 / null 不限")
    private Boolean financed;

    @Schema(description = "成立日期起（yyyy-MM-dd）", example = "2015-01-01")
    private String establishDateStart;

    @Schema(description = "成立日期止（yyyy-MM-dd）", example = "2024-12-31")
    private String establishDateEnd;

    @Schema(description = "一级行业名（多选，逗号分隔）", example = "制造业,信息传输、软件和信息技术服务业")
    private String industryLv1Names;

    @Schema(description = "二级行业名（多选，逗号分隔）", example = "软件和信息技术服务业")
    private String industryLv2Names;

    @Schema(description = "实缴资本下限（万元，取 reg_capital_amount）", example = "100")
    private BigDecimal actualCapitalMin;

    @Schema(description = "实缴资本上限（万元，取 reg_capital_amount）", example = "5000")
    private BigDecimal actualCapitalMax;

    /** 是否所有画像条件都为空（为空时等价于不做画像过滤） */
    public boolean isEmpty() {
        return insuredCountMin == null && insuredCountMax == null
                && financed == null
                && isBlank(establishDateStart) && isBlank(establishDateEnd)
                && isBlank(industryLv1Names) && isBlank(industryLv2Names)
                && actualCapitalMin == null && actualCapitalMax == null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

}
