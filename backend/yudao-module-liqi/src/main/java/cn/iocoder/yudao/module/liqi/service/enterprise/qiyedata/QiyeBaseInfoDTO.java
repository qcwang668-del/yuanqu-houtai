package cn.iocoder.yudao.module.liqi.service.enterprise.qiyedata;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 企业数据平台「企业基本信息」响应体（1:1 对齐 QiYeInfoNewVO）。
 */
@Data
public class QiyeBaseInfoDTO {

    /** 统一社会信用代码/唯一 id */
    private String entityId;
    /** 公司名称 */
    private String companyName;
    /** 公司曾用名 */
    private String companyFormerName;
    /** 企业英文名称 */
    private String entityEnglishName;
    /** 网址 */
    private String zcbWeb;
    /** 邮箱 */
    private String zcbEmail;
    /** 法定代表人 */
    private String legalName;
    /** 注册资本 */
    private BigDecimal regCapital;
    /** 注册资本币种 */
    private String regCapitalType;
    /** 经营状态 */
    private String entStatus;
    /** 注册地址 */
    private String regAddress;
    /** 行业一级分类编码 */
    private Integer industryLv1;
    /** 行业一级分类名 */
    private String industryLv1Name;
    /** 行业二级分类 */
    private Integer industryLv2;
    /** 行业二级分类名 */
    private String industryLv2Name;
    /** 行业三级分类 */
    private Integer industryLv3;
    /** 行业三级分类名 */
    private String industryLv3Name;
    /** 行业四级分类 */
    private Integer industryLv4;
    /** 行业四级分类名 */
    private String industryLv4Name;
    /** 成立日期 */
    private String regDate;
    /** 工商注册号 */
    private String licenseNumber;
    /** 企业类型 */
    private String zcbComType;
    /** 经营期限自 */
    private String opFrom;
    /** 经营期限至 */
    private String opTo;
    /** 登记机关 */
    private String regOrg;
    /** 核准日期 */
    private String checkDate;
    /** 经营范围 */
    private String opScope;
    /** 省码 */
    private Integer regProvincesCode;
    /** 市码 */
    private Integer regCityCode;
    /** 区码 */
    private Integer regDistrictCode;
    /** 补贴总金额 */
    private BigDecimal subsidyTotalMoney;
    /** 参保人数（人员规模） */
    private Integer socialStaffNum;
    /** 组织机构代码 */
    private String organizationNumber;
    /** 企业规模：1 微型 2 小型 3 中型 4 大型 */
    private Integer companyScale;
    /** 最终展示信息（荣誉、资质等） */
    private List<String> finalShowInfo;

}
