package cn.iocoder.yudao.module.liqi.controller.admin.enterprise.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 企业库分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EnterprisePageReqVO extends PageParam {

    @Schema(description = "企业名称")
    private String enterpriseName;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "注册地址（园区关键词）")
    private String registerAddress;

    @Schema(description = "行业一级分类名（大屏/地图招商筛选）")
    private String industryLv1Name;

    @Schema(description = "经营状态（存续/注销/吊销…，前缀匹配）")
    private String regStatus;

    @Schema(description = "企业类型（有限责任公司/股份有限公司…）")
    private String companyOrgType;

    @Schema(description = "企业规模：1微型 2小型 3中型 4大型")
    private Integer companyScale;

    @Schema(description = "参保人数下限")
    private Integer insuredCountMin;

    @Schema(description = "参保人数上限")
    private Integer insuredCountMax;

    @Schema(description = "注册资本下限（万元）")
    private java.math.BigDecimal regCapitalMin;

    @Schema(description = "注册资本上限（万元）")
    private java.math.BigDecimal regCapitalMax;

    @Schema(description = "成立日期起（yyyy-MM-dd）")
    private String establishDateStart;

    @Schema(description = "成立日期止（yyyy-MM-dd）")
    private String establishDateEnd;

    @Schema(description = "区码（440305 南山区…）")
    private Integer regDistrictCode;

    @Schema(description = "排序字段：insuredCount / regCapitalAmount / establishDate / id")
    private String sortField;

    @Schema(description = "是否倒序，默认 false")
    private Boolean sortDesc;

}
