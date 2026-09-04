package cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 力企 - 企业库 DO（深圳湾生态园企业）
 */
@TableName("liqi_enterprise")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiEnterpriseDO extends TenantBaseDO {

    @TableId
    private Long id;
    /** 企业名称 */
    private String enterpriseName;
    /** 法定代表人 */
    private String legalPerson;
    /** 成立时间 */
    private LocalDate establishDate;
    /** 注册资本 */
    private String registeredCapital;
    /** 参保人数 */
    private Integer insuredCount;
    /** 所属行业 */
    private String industry;
    /** 注册地址 */
    private String registerAddress;
    /** logo 简称 */
    private String shortName;
    /** logo 颜色 */
    private String logoColor;

    // ========== 基本信息扩展维度（天眼查按需回填）==========
    /** 统一社会信用代码 */
    private String creditCode;
    /** 工商注册号 */
    private String regNumber;
    /** 组织机构代码 */
    private String orgNumber;
    /** 纳税人识别号 */
    private String taxNumber;
    /** 曾用名 */
    private String formerName;
    /** 企业类型 */
    private String companyOrgType;
    /** 经营状态 */
    private String regStatus;
    /** 实缴资本 */
    private String actualCapital;
    /** 人员规模 */
    private String staffNumRange;
    /** 登记机关 */
    private String regInstitute;
    /** 上市简称 */
    private String bondName;
    /** 上市代码 */
    private String bondNum;
    /** 经营范围 */
    private String businessScope;
    /** 企业标签（; 分隔） */
    private String tags;
    /** 工商回填状态：0未回填 1已回填 2查无结果 */
    private Integer enrichStatus;
    /** 最近工商回填时间 */
    private LocalDateTime enrichTime;
    /** 最近回填数据源：qiyedata（企业数据平台）/ tyc（天眼查） */
    private String enrichSource;

    // ========== 企业数据平台「企业基本信息」接口字段（qiye-base-info）==========
    /** 平台 entityId（统一社会信用代码/唯一 id） */
    private String entityId;
    /** 企业英文名称 */
    private String englishName;
    /** 网址 */
    private String website;
    /** 邮箱 */
    private String email;
    /** 注册资本金额 */
    private BigDecimal regCapitalAmount;
    /** 注册资本币种 */
    private String regCapitalType;
    /** 行业一级分类编码 */
    private Integer industryLv1;
    /** 行业一级分类名 */
    private String industryLv1Name;
    /** 行业二级分类编码 */
    private Integer industryLv2;
    /** 行业二级分类名 */
    private String industryLv2Name;
    /** 行业三级分类编码 */
    private Integer industryLv3;
    /** 行业三级分类名 */
    private String industryLv3Name;
    /** 行业四级分类编码 */
    private Integer industryLv4;
    /** 行业四级分类名 */
    private String industryLv4Name;
    /** 经营期限自 */
    private String opFrom;
    /** 经营期限至 */
    private String opTo;
    /** 核准日期 */
    private String checkDate;
    /** 省码 */
    private Integer regProvincesCode;
    /** 市码 */
    private Integer regCityCode;
    /** 区码 */
    private Integer regDistrictCode;
    /** 补贴总金额 */
    private BigDecimal subsidyTotalMoney;
    /** 企业规模：1微型 2小型 3中型 4大型 */
    private Integer companyScale;
    /** 最终展示信息（荣誉/资质等，; 分隔） */
    private String finalShowInfo;

}
