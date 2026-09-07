package cn.iocoder.yudao.module.liqi.service.enterprise;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.liqi.controller.admin.enterprise.vo.EnterprisePageReqVO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseDO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseShareholderDO;
import cn.iocoder.yudao.module.liqi.dal.mysql.enterprise.LiqiEnterpriseMapper;
import cn.iocoder.yudao.module.liqi.dal.mysql.enterprise.LiqiEnterpriseContactMapper;
import cn.iocoder.yudao.module.liqi.dal.mysql.enterprise.LiqiEnterpriseFinancingMapper;
import cn.iocoder.yudao.module.liqi.dal.mysql.enterprise.LiqiEnterpriseIpMapper;
import cn.iocoder.yudao.module.liqi.dal.mysql.enterprise.LiqiEnterpriseProjectMapper;
import cn.iocoder.yudao.module.liqi.dal.mysql.enterprise.LiqiEnterpriseRiskMapper;
import cn.iocoder.yudao.module.liqi.dal.mysql.enterprise.LiqiEnterpriseShareholderMapper;
import cn.iocoder.yudao.module.liqi.service.enterprise.qiyedata.QiyeBaseInfoDTO;
import cn.iocoder.yudao.module.liqi.service.enterprise.qiyedata.QiyeDataClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 力企 - 企业库 Service 实现类
 */
@Slf4j
@Service
@Validated
public class LiqiEnterpriseServiceImpl implements LiqiEnterpriseService {

    /** 回填数据源标识 */
    private static final String SOURCE_QIYEDATA = "qiyedata";
    private static final String SOURCE_TYC = "tyc";

    @Resource
    private LiqiEnterpriseMapper enterpriseMapper;
    @Resource
    private LiqiEnterpriseShareholderMapper shareholderMapper;
    @Resource
    private LiqiEnterpriseContactMapper contactMapper;
    @Resource
    private LiqiEnterpriseIpMapper ipMapper;
    @Resource
    private LiqiEnterpriseRiskMapper riskMapper;
    @Resource
    private LiqiEnterpriseProjectMapper projectMapper;
    @Resource
    private LiqiEnterpriseFinancingMapper financingMapper;
    @Resource
    private TycClient tycClient;
    @Resource
    private QiyeDataClient qiyeDataClient;

    @Override
    public PageResult<LiqiEnterpriseDO> getEnterprisePage(EnterprisePageReqVO pageReqVO) {
        return enterpriseMapper.selectPage(pageReqVO);
    }

    @Override
    public Map<String, Object> getEnterpriseDetail(Long id) {
        return buildDetail(id == null ? null : enterpriseMapper.selectById(id));
    }

    @Override
    public Map<String, Object> getEnterpriseDetailByName(String enterpriseName) {
        return buildDetail(StrUtil.isBlank(enterpriseName) ? null
                : enterpriseMapper.selectByEnterpriseName(enterpriseName.trim()));
    }

    /** 组装企业详情：主表 + 各子表；企业不存在时子表统一返回空集合 */
    private Map<String, Object> buildDetail(LiqiEnterpriseDO ent) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enterprise", ent);
        if (ent == null) {
            result.put("shareholders", new ArrayList<>());
            result.put("contacts", new ArrayList<>());
            result.put("ips", new ArrayList<>());
            result.put("risks", new ArrayList<>());
            result.put("projects", new ArrayList<>());
            result.put("financing", new ArrayList<>());
            return result;
        }
        // 各子表均取库内真实数据，无数据时前端展示空态（不再使用示例/演示数据）
        Long entId = ent.getId();
        result.put("shareholders", shareholderMapper.selectListByEnterpriseId(entId));
        result.put("contacts", contactMapper.selectListByEnterpriseId(entId));
        result.put("ips", ipMapper.selectListByEnterpriseId(entId));
        result.put("risks", riskMapper.selectListByEnterpriseId(entId));
        result.put("projects", projectMapper.selectListByEnterpriseId(entId));
        result.put("financing", financingMapper.selectListByEnterpriseId(entId));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> enrich(Long id) {
        LiqiEnterpriseDO ent = enterpriseMapper.selectById(id);
        if (ent == null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("code", 404);
            r.put("msg", "企业不存在");
            return r;
        }
        // 关键词优先用统一社会信用代码，否则用工商全称
        String keyword = StrUtil.isNotBlank(ent.getCreditCode()) ? ent.getCreditCode() : ent.getEnterpriseName();
        JSONObject base = tycClient.baseInfo(keyword);
        if (base == null) {
            // 查无结果（多为虚构名/简称）
            ent.setEnrichStatus(2);
            ent.setEnrichTime(LocalDateTime.now());
            enterpriseMapper.updateById(ent);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("code", 0);
            r.put("enriched", false);
            r.put("msg", "天眼查查无结果（需工商登记全称/统一社会信用代码）");
            r.putAll(getEnterpriseDetail(id));
            return r;
        }
        // 映射工商基本信息 → 主表扩展列
        ent.setLegalPerson(strOr(base.getStr("legalPersonName"), ent.getLegalPerson()));
        ent.setRegisteredCapital(strOr(base.getStr("regCapital"), ent.getRegisteredCapital()));
        ent.setIndustry(strOr(base.getStr("industry"), ent.getIndustry()));
        ent.setRegisterAddress(strOr(base.getStr("regLocation"), ent.getRegisterAddress()));
        ent.setCreditCode(base.getStr("creditCode"));
        ent.setRegNumber(base.getStr("regNumber"));
        ent.setOrgNumber(base.getStr("orgNumber"));
        ent.setTaxNumber(base.getStr("taxNumber"));
        ent.setFormerName(base.getStr("historyNames"));
        ent.setCompanyOrgType(base.getStr("companyOrgType"));
        ent.setRegStatus(base.getStr("regStatus"));
        ent.setActualCapital(base.getStr("actualCapital"));
        ent.setStaffNumRange(base.getStr("staffNumRange"));
        ent.setRegInstitute(base.getStr("regInstitute"));
        ent.setBondName(base.getStr("bondName"));
        ent.setBondNum(base.getStr("bondNum"));
        ent.setBusinessScope(base.getStr("businessScope"));
        ent.setTags(base.getStr("tags"));
        Integer socialStaffNum = base.getInt("socialStaffNum");
        if (socialStaffNum != null) {
            ent.setInsuredCount(socialStaffNum);
        }
        ent.setEnrichStatus(1);
        ent.setEnrichTime(LocalDateTime.now());
        ent.setEnrichSource(SOURCE_TYC);
        enterpriseMapper.updateById(ent);
        // 股东：全量替换
        shareholderMapper.deleteByEnterpriseId(id);
        List<String[]> holders = tycClient.holders(keyword);
        for (String[] h : holders) {
            shareholderMapper.insert(LiqiEnterpriseShareholderDO.builder()
                    .enterpriseId(id).name(h[0]).percent(h[1]).build());
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 0);
        r.put("enriched", true);
        r.put("msg", "回填成功");
        r.putAll(getEnterpriseDetail(id));
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncBaseInfo(Long id) {
        Map<String, Object> r = new LinkedHashMap<>();
        LiqiEnterpriseDO ent = enterpriseMapper.selectById(id);
        if (ent == null) {
            r.put("code", 404);
            r.put("synced", false);
            r.put("msg", "企业不存在");
            return r;
        }
        // entityId 只从库内取（entity_id 优先，其次统一社会信用代码），不接受外部直传，避免成为该接口的查询代理
        String entityId = StrUtil.isNotBlank(ent.getEntityId()) ? ent.getEntityId() : ent.getCreditCode();
        if (StrUtil.isBlank(entityId)) {
            r.put("code", 0);
            r.put("synced", false);
            r.put("msg", "该企业缺少 entityId/统一社会信用代码，无法调用企业基本信息接口");
            r.putAll(getEnterpriseDetail(id));
            return r;
        }
        QiyeBaseInfoDTO info = qiyeDataClient.baseInfo(entityId);
        if (info == null) {
            ent.setEnrichStatus(2);
            ent.setEnrichTime(LocalDateTime.now());
            ent.setEnrichSource(SOURCE_QIYEDATA);
            enterpriseMapper.updateById(ent);
            r.put("code", 0);
            r.put("synced", false);
            r.put("msg", "企业数据平台查无结果或接口不可用");
            r.putAll(getEnterpriseDetail(id));
            return r;
        }
        applyBaseInfo(ent, info);
        enterpriseMapper.updateById(ent);
        r.put("code", 0);
        r.put("synced", true);
        r.put("msg", "企业基本信息同步成功");
        r.putAll(getEnterpriseDetail(id));
        return r;
    }

    @Override
    public Map<String, Object> syncBaseInfoBatch(int limit, long intervalMs) {
        int max = limit <= 0 ? 100 : Math.min(limit, 2000);
        long interval = Math.max(0L, intervalMs);
        List<LiqiEnterpriseDO> pending = enterpriseMapper.selectPendingForSync(max);
        int succeed = 0;
        int notFound = 0;
        List<Map<String, Object>> failures = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            LiqiEnterpriseDO ent = pending.get(i);
            String entityId = StrUtil.isNotBlank(ent.getEntityId()) ? ent.getEntityId() : ent.getCreditCode();
            try {
                QiyeBaseInfoDTO info = qiyeDataClient.baseInfo(entityId);
                if (info == null) {
                    ent.setEnrichStatus(2);
                    ent.setEnrichTime(LocalDateTime.now());
                    ent.setEnrichSource(SOURCE_QIYEDATA);
                    enterpriseMapper.updateById(ent);
                    notFound++;
                } else {
                    applyBaseInfo(ent, info);
                    enterpriseMapper.updateById(ent);
                    succeed++;
                }
            } catch (Exception e) {
                log.warn("[syncBaseInfoBatch] 企业补全失败 id={} entityId={}", ent.getId(), entityId, e);
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("id", ent.getId());
                f.put("enterpriseName", ent.getEnterpriseName());
                f.put("error", e.getMessage());
                failures.add(f);
            }
            // 限流：最后一条不再等待
            if (interval > 0 && i < pending.size() - 1) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("[syncBaseInfoBatch] 被中断，提前结束本批");
                    break;
                }
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 0);
        r.put("picked", pending.size());
        r.put("succeed", succeed);
        r.put("notFound", notFound);
        r.put("failed", failures.size());
        r.put("failures", failures);
        r.putAll(getEnrichProgress());
        return r;
    }

    @Override
    public Map<String, Object> getEnrichProgress() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", enterpriseMapper.selectCount(null));
        r.put("enriched", enterpriseMapper.selectCountByEnrichStatus(1));
        r.put("notFound", enterpriseMapper.selectCountByEnrichStatus(2));
        r.put("pending", enterpriseMapper.selectCountByEnrichStatus(0));
        r.put("syncable", (long) enterpriseMapper.selectPendingForSync(Integer.MAX_VALUE).size());
        return r;
    }

    /** 接口返回字段 → 企业库主表列（语义相同的已有列复用，其余落新增列） */
    private static void applyBaseInfo(LiqiEnterpriseDO ent, QiyeBaseInfoDTO info) {
        ent.setEntityId(strOr(info.getEntityId(), ent.getEntityId()));
        // entityId 语义为「统一社会信用代码/唯一id」：仅当其确为 18 位信用代码时才回写 credit_code，
        // 否则（平台内部数字 id）保留库内原有信用代码，避免被内部 id 覆盖
        if (isCreditCode(info.getEntityId())) {
            ent.setCreditCode(info.getEntityId());
        }
        ent.setEnterpriseName(strOr(info.getCompanyName(), ent.getEnterpriseName()));
        ent.setFormerName(strOr(info.getCompanyFormerName(), ent.getFormerName()));
        ent.setEnglishName(strOr(info.getEntityEnglishName(), ent.getEnglishName()));
        ent.setWebsite(strOr(info.getZcbWeb(), ent.getWebsite()));
        ent.setEmail(strOr(info.getZcbEmail(), ent.getEmail()));
        ent.setLegalPerson(strOr(info.getLegalName(), ent.getLegalPerson()));
        ent.setRegCapitalAmount(info.getRegCapital() != null ? info.getRegCapital() : ent.getRegCapitalAmount());
        ent.setRegCapitalType(strOr(info.getRegCapitalType(), ent.getRegCapitalType()));
        ent.setRegisteredCapital(strOr(formatCapital(info.getRegCapital(), info.getRegCapitalType()),
                ent.getRegisteredCapital()));
        ent.setRegStatus(strOr(info.getEntStatus(), ent.getRegStatus()));
        ent.setRegisterAddress(strOr(info.getRegAddress(), ent.getRegisterAddress()));
        ent.setIndustryLv1(info.getIndustryLv1() != null ? info.getIndustryLv1() : ent.getIndustryLv1());
        ent.setIndustryLv1Name(strOr(info.getIndustryLv1Name(), ent.getIndustryLv1Name()));
        ent.setIndustryLv2(info.getIndustryLv2() != null ? info.getIndustryLv2() : ent.getIndustryLv2());
        ent.setIndustryLv2Name(strOr(info.getIndustryLv2Name(), ent.getIndustryLv2Name()));
        ent.setIndustryLv3(info.getIndustryLv3() != null ? info.getIndustryLv3() : ent.getIndustryLv3());
        ent.setIndustryLv3Name(strOr(info.getIndustryLv3Name(), ent.getIndustryLv3Name()));
        ent.setIndustryLv4(info.getIndustryLv4() != null ? info.getIndustryLv4() : ent.getIndustryLv4());
        ent.setIndustryLv4Name(strOr(info.getIndustryLv4Name(), ent.getIndustryLv4Name()));
        ent.setIndustry(strOr(joinIndustry(info), ent.getIndustry()));
        LocalDate regDate = parseDate(info.getRegDate());
        if (regDate != null) {
            ent.setEstablishDate(regDate);
        }
        ent.setRegNumber(strOr(info.getLicenseNumber(), ent.getRegNumber()));
        ent.setCompanyOrgType(strOr(info.getZcbComType(), ent.getCompanyOrgType()));
        ent.setOpFrom(strOr(info.getOpFrom(), ent.getOpFrom()));
        ent.setOpTo(strOr(info.getOpTo(), ent.getOpTo()));
        ent.setRegInstitute(strOr(info.getRegOrg(), ent.getRegInstitute()));
        ent.setCheckDate(strOr(info.getCheckDate(), ent.getCheckDate()));
        ent.setBusinessScope(strOr(info.getOpScope(), ent.getBusinessScope()));
        ent.setRegProvincesCode(info.getRegProvincesCode() != null ? info.getRegProvincesCode() : ent.getRegProvincesCode());
        ent.setRegCityCode(info.getRegCityCode() != null ? info.getRegCityCode() : ent.getRegCityCode());
        ent.setRegDistrictCode(info.getRegDistrictCode() != null ? info.getRegDistrictCode() : ent.getRegDistrictCode());
        ent.setSubsidyTotalMoney(info.getSubsidyTotalMoney() != null ? info.getSubsidyTotalMoney() : ent.getSubsidyTotalMoney());
        if (info.getSocialStaffNum() != null) {
            ent.setInsuredCount(info.getSocialStaffNum());
            ent.setStaffNumRange(String.valueOf(info.getSocialStaffNum()));
        }
        ent.setOrgNumber(strOr(info.getOrganizationNumber(), ent.getOrgNumber()));
        ent.setCompanyScale(info.getCompanyScale() != null ? info.getCompanyScale() : ent.getCompanyScale());
        String showInfo = joinList(info.getFinalShowInfo());
        ent.setFinalShowInfo(strOr(showInfo, ent.getFinalShowInfo()));
        // 荣誉/资质同时作为企业标签展示（前端标签区复用 tags）
        ent.setTags(strOr(showInfo, ent.getTags()));
        ent.setEnrichStatus(1);
        ent.setEnrichTime(LocalDateTime.now());
        ent.setEnrichSource(SOURCE_QIYEDATA);
    }

    /** 行业分类：优先取最细一级，拼成「一级/末级」 */
    private static String joinIndustry(QiyeBaseInfoDTO info) {
        String lv1 = info.getIndustryLv1Name();
        String last = StrUtil.isNotBlank(info.getIndustryLv4Name()) ? info.getIndustryLv4Name()
                : StrUtil.isNotBlank(info.getIndustryLv3Name()) ? info.getIndustryLv3Name()
                : info.getIndustryLv2Name();
        if (StrUtil.isBlank(lv1)) {
            return last;
        }
        return StrUtil.isBlank(last) || lv1.equals(last) ? lv1 : lv1 + "/" + last;
    }

    /** 注册资本展示值：金额 + 单位 + 币种，如 regCapital=300、regCapitalType="人民币(单位：万元)" → "300万元人民币" */
    private static String formatCapital(BigDecimal amount, String type) {
        if (amount == null) {
            return null;
        }
        String number = amount.stripTrailingZeros().toPlainString();
        if (StrUtil.isBlank(type)) {
            return number;
        }
        // 币种形如 "人民币(单位：万元)"：拆出币种与金额单位，拼成「300万元人民币」
        String unit = StrUtil.subBetween(type, "：", ")");
        if (StrUtil.isBlank(unit)) {
            unit = StrUtil.subBetween(type, ":", ")");
        }
        String currency = StrUtil.subBefore(type, "(", false);
        if (StrUtil.isBlank(currency)) {
            currency = StrUtil.subBefore(type, "（", false);
        }
        if (StrUtil.isNotBlank(unit)) {
            return number + StrUtil.trim(unit) + StrUtil.trim(StrUtil.blankToDefault(currency, ""));
        }
        return number + StrUtil.trim(type);
    }

    private static String joinList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream().filter(StrUtil::isNotBlank).collect(Collectors.joining(";"));
    }

    /** 成立日期：接口为 string(date)，兼容 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss */
    private static LocalDate parseDate(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String value = StrUtil.subBefore(text.trim(), " ", false);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 统一社会信用代码：18 位大写字母 + 数字 */
    private static boolean isCreditCode(String value) {
        return value != null && value.length() == 18 && value.matches("^[0-9A-Z]{18}$");
    }

    private static String strOr(String v, String fallback) {
        return StrUtil.isNotBlank(v) ? v : fallback;
    }

    /** 企业规模字典：company_scale → 展示名 */
    private static final Map<String, String> SCALE_NAMES = new LinkedHashMap<String, String>() {{
        put("1", "微型");
        put("2", "小型");
        put("3", "中型");
        put("4", "大型");
    }};

    @Override
    public Map<String, Object> getEnterpriseStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", enterpriseMapper.selectCount(null));
        result.put("industries", enterpriseMapper.selectIndustryCount());
        // 一级行业分布（企业数据平台 industryLv1Name，供大屏赛道环图）
        result.put("industryLv1", enterpriseMapper.selectIndustryLv1Count());
        // 企业规模分布：把编码翻成中文名
        List<Map<String, Object>> scales = new ArrayList<>();
        for (Map<String, Object> row : enterpriseMapper.selectScaleCount()) {
            Map<String, Object> item = new LinkedHashMap<>();
            String code = row.get("name") == null ? "" : String.valueOf(row.get("name"));
            item.put("name", SCALE_NAMES.getOrDefault(code, "未知"));
            item.put("value", row.get("value"));
            scales.add(item);
        }
        result.put("scales", scales);
        // 经营状态分布 / 成立年份趋势
        result.put("statuses", enterpriseMapper.selectStatusCount());
        result.put("yearTrend", enterpriseMapper.selectYearTrend());
        // 参保人数 Top 10（榜单）
        result.put("topList", buildTop(enterpriseMapper.selectTop(10), false));
        // 注册资本 Top 10（榜单）
        result.put("topCapital", buildTop(enterpriseMapper.selectTopByCapital(10), true));
        // 补全进度，便于大屏标注数据完整度
        result.put("enriched", enterpriseMapper.selectCountByEnrichStatus(1));
        result.put("notFound", enterpriseMapper.selectCountByEnrichStatus(2));
        return result;
    }

    /** 榜单行：id/name/value(+capital 时取注册资本金额) */
    private List<Map<String, Object>> buildTop(List<LiqiEnterpriseDO> list, boolean byCapital) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LiqiEnterpriseDO e : list) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", e.getId());
            t.put("name", e.getEnterpriseName());
            t.put("industry", e.getIndustryLv1Name());
            t.put("value", byCapital ? e.getRegCapitalAmount() : e.getInsuredCount());
            t.put("text", byCapital ? e.getRegisteredCapital()
                    : (e.getInsuredCount() == null ? "-" : e.getInsuredCount() + " 人"));
            rows.add(t);
        }
        return rows;
    }

}
