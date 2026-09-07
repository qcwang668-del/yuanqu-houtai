package cn.iocoder.yudao.module.liqi.dal.mysql.enterprise;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.controller.admin.enterprise.vo.EnterprisePageReqVO;
import cn.iocoder.yudao.module.liqi.message.controller.admin.vo.ParkPushFilterVO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * 力企 - 企业库 Mapper
 */
@Mapper
public interface LiqiEnterpriseMapper extends BaseMapperX<LiqiEnterpriseDO> {

    default PageResult<LiqiEnterpriseDO> selectPage(EnterprisePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LiqiEnterpriseDO>()
                .likeIfPresent(LiqiEnterpriseDO::getEnterpriseName, reqVO.getEnterpriseName())
                .likeIfPresent(LiqiEnterpriseDO::getIndustry, reqVO.getIndustry())
                .likeIfPresent(LiqiEnterpriseDO::getRegisterAddress, reqVO.getRegisterAddress())
                .likeIfPresent(LiqiEnterpriseDO::getIndustryLv1Name, reqVO.getIndustryLv1Name())
                .likeIfPresent(LiqiEnterpriseDO::getRegStatus, reqVO.getRegStatus())
                .likeIfPresent(LiqiEnterpriseDO::getCompanyOrgType, reqVO.getCompanyOrgType())
                .eqIfPresent(LiqiEnterpriseDO::getCompanyScale, reqVO.getCompanyScale())
                .geIfPresent(LiqiEnterpriseDO::getInsuredCount, reqVO.getInsuredCountMin())
                .leIfPresent(LiqiEnterpriseDO::getInsuredCount, reqVO.getInsuredCountMax())
                .geIfPresent(LiqiEnterpriseDO::getRegCapitalAmount, reqVO.getRegCapitalMin())
                .leIfPresent(LiqiEnterpriseDO::getRegCapitalAmount, reqVO.getRegCapitalMax())
                .geIfPresent(LiqiEnterpriseDO::getEstablishDate, reqVO.getEstablishDateStart())
                .leIfPresent(LiqiEnterpriseDO::getEstablishDate, reqVO.getEstablishDateEnd())
                .eqIfPresent(LiqiEnterpriseDO::getRegDistrictCode, reqVO.getRegDistrictCode())
                // 是否融资：按融资子表有无记录过滤（未传则不限）
                .exists(Boolean.TRUE.equals(reqVO.getFinanced()),
                        "SELECT 1 FROM liqi_enterprise_financing f"
                                + " WHERE f.enterprise_id = liqi_enterprise.id AND f.deleted = 0")
                .notExists(Boolean.FALSE.equals(reqVO.getFinanced()),
                        "SELECT 1 FROM liqi_enterprise_financing f"
                                + " WHERE f.enterprise_id = liqi_enterprise.id AND f.deleted = 0")
                .orderByAsc(LiqiEnterpriseDO::getId));
    }

    /** 行业分布（name=行业, value=数量），倒序 */
    default List<Map<String, Object>> selectIndustryCount() {
        return selectMaps(new QueryWrapper<LiqiEnterpriseDO>()
                .select("industry AS name, count(*) AS value")
                .groupBy("industry")
                .orderByDesc("count(*)"));
    }

    /** 一级行业分布 */
    default List<Map<String, Object>> selectIndustryLv1Count() {
        return selectMaps(new QueryWrapper<LiqiEnterpriseDO>()
                .select("industry_lv1_name AS name, count(*) AS value")
                .isNotNull("industry_lv1_name")
                .groupBy("industry_lv1_name")
                .orderByDesc("count(*)"));
    }

    /** 企业规模分布 */
    default List<Map<String, Object>> selectScaleCount() {
        return selectMaps(new QueryWrapper<LiqiEnterpriseDO>()
                .select("company_scale AS name, count(*) AS value")
                .isNotNull("company_scale")
                .groupBy("company_scale")
                .orderByDesc("count(*)"));
    }

    /** 经营状态分布 */
    default List<Map<String, Object>> selectStatusCount() {
        return selectMaps(new QueryWrapper<LiqiEnterpriseDO>()
                .select("reg_status AS name, count(*) AS value")
                .isNotNull("reg_status")
                .groupBy("reg_status")
                .orderByDesc("count(*)"));
    }

    /** 成立年份趋势 */
    default List<Map<String, Object>> selectYearTrend() {
        return selectMaps(new QueryWrapper<LiqiEnterpriseDO>()
                .select("YEAR(establish_date) AS name, count(*) AS value")
                .isNotNull("establish_date")
                .groupBy("YEAR(establish_date)")
                .orderByAsc("YEAR(establish_date)"));
    }

    /** 参保人数 Top N 企业（当作榜单） */
    default List<LiqiEnterpriseDO> selectTop(int n) {
        return selectList(new LambdaQueryWrapperX<LiqiEnterpriseDO>()
                .orderByDesc(LiqiEnterpriseDO::getInsuredCount)
                .last("limit " + n));
    }

    /** 注册资本 Top N 企业 */
    default List<LiqiEnterpriseDO> selectTopByCapital(int n) {
        return selectList(new LambdaQueryWrapperX<LiqiEnterpriseDO>()
                .orderByDesc(LiqiEnterpriseDO::getRegCapitalAmount)
                .last("limit " + n));
    }

    /**
     * 取待补全企业（enrich_status=0 未回填，且有 entityId 或统一社会信用代码），按 id 升序。
     * 供批量同步任务分批拉取，天然支持断点续跑（已成功的记录状态被改写，不会再次命中）。
     */
    default List<LiqiEnterpriseDO> selectPendingForSync(int limit) {
        return selectList(new QueryWrapper<LiqiEnterpriseDO>()
                .eq("enrich_status", 0)
                .and(w -> w.isNotNull("entity_id").ne("entity_id", "")
                        .or().isNotNull("credit_code").ne("credit_code", ""))
                .orderByAsc("id")
                .last("limit " + limit));
    }

    /** 按回填状态统计数量 */
    default Long selectCountByEnrichStatus(Integer status) {
        return selectCount(new LambdaQueryWrapperX<LiqiEnterpriseDO>()
                .eqIfPresent(LiqiEnterpriseDO::getEnrichStatus, status));
    }

    /**
     * 按企业名称精确查询企业。
     *
     * <p>会员/会员线索/获批动态等业务表与企业库之间没有外键，只能按企业名称关联；
     * 同名多条时取 id 最小的一条，保证结果稳定。</p>
     */
    default LiqiEnterpriseDO selectByEnterpriseName(String enterpriseName) {
        return selectOne(new LambdaQueryWrapperX<LiqiEnterpriseDO>()
                .eq(LiqiEnterpriseDO::getEnterpriseName, enterpriseName)
                .orderByAsc(LiqiEnterpriseDO::getId)
                .last("limit 1"));
    }

    /**
     * 园区发布项目「客户对象」画像圈选：在给定企业名称范围内，按企业画像条件过滤。
     *
     * <p>圈选范围由调用方传入（园区客户管理 liqi_member 中本园区的企业名称集合），
     * 本方法只负责在该范围内做画像过滤。企业库与会员表之间无外键，按企业名称关联，
     * 口径与 {@link #selectByEnterpriseName} 一致。</p>
     *
     * @param filter          画像筛选条件；条件为空的项不参与过滤
     * @param enterpriseNames 圈选范围内的企业名称集合；为空时直接返回空列表
     */
    default List<LiqiEnterpriseDO> selectByPushFilter(ParkPushFilterVO filter,
                                                      Collection<String> enterpriseNames) {
        if (enterpriseNames == null || enterpriseNames.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        ParkPushFilterVO f = filter == null ? new ParkPushFilterVO() : filter;
        return selectList(new LambdaQueryWrapperX<LiqiEnterpriseDO>()
                .in(LiqiEnterpriseDO::getEnterpriseName, enterpriseNames)
                // 参保人数区间
                .geIfPresent(LiqiEnterpriseDO::getInsuredCount, f.getInsuredCountMin())
                .leIfPresent(LiqiEnterpriseDO::getInsuredCount, f.getInsuredCountMax())
                // 成立日期区间
                .geIfPresent(LiqiEnterpriseDO::getEstablishDate, f.getEstablishDateStart())
                .leIfPresent(LiqiEnterpriseDO::getEstablishDate, f.getEstablishDateEnd())
                // 实缴资本区间（复用工商登记注册资本金额字段）
                .geIfPresent(LiqiEnterpriseDO::getRegCapitalAmount, f.getActualCapitalMin())
                .leIfPresent(LiqiEnterpriseDO::getRegCapitalAmount, f.getActualCapitalMax())
                // 所属行业（一级 / 二级，均支持多选）
                .inIfPresent(LiqiEnterpriseDO::getIndustryLv1Name, splitCsv(f.getIndustryLv1Names()))
                .inIfPresent(LiqiEnterpriseDO::getIndustryLv2Name, splitCsv(f.getIndustryLv2Names()))
                // 是否有融资：按融资子表有无记录过滤（未传则不限）
                .exists(Boolean.TRUE.equals(f.getFinanced()),
                        "SELECT 1 FROM liqi_enterprise_financing fin"
                                + " WHERE fin.enterprise_id = liqi_enterprise.id AND fin.deleted = 0")
                .notExists(Boolean.FALSE.equals(f.getFinanced()),
                        "SELECT 1 FROM liqi_enterprise_financing fin"
                                + " WHERE fin.enterprise_id = liqi_enterprise.id AND fin.deleted = 0")
                .orderByAsc(LiqiEnterpriseDO::getId));
    }

    /** 查询有融资记录的企业 id 集合（在给定企业 id 范围内），用于候选名单回填「是否有融资」 */
    default List<Long> selectFinancedIds(Collection<Long> enterpriseIds) {
        if (enterpriseIds == null || enterpriseIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<LiqiEnterpriseDO>()
                .select(LiqiEnterpriseDO::getId)
                .in(LiqiEnterpriseDO::getId, enterpriseIds)
                .exists("SELECT 1 FROM liqi_enterprise_financing fin"
                        + " WHERE fin.enterprise_id = liqi_enterprise.id AND fin.deleted = 0"))
                .stream().map(LiqiEnterpriseDO::getId).collect(Collectors.toList());
    }

    /** 二级行业选项：按一级/二级行业分组去重，供发布页行业级联选择器使用 */
    default List<Map<String, Object>> selectIndustryLv2Options() {
        return selectMaps(new QueryWrapper<LiqiEnterpriseDO>()
                .select("industry_lv1_name AS lv1, industry_lv2_name AS lv2, count(*) AS value")
                .isNotNull("industry_lv1_name")
                .ne("industry_lv1_name", "")
                .groupBy("industry_lv1_name", "industry_lv2_name")
                .orderByAsc("industry_lv1_name", "industry_lv2_name"));
    }

    /** 逗号分隔字符串 → 去空去重列表；为空返回 null（配合 inIfPresent 跳过该条件） */
    static List<String> splitCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return null;
        }
        List<String> list = Arrays.stream(csv.split("[,，]"))
                .map(String::trim).filter(s -> !s.isEmpty())
                .distinct().collect(Collectors.toList());
        return list.isEmpty() ? null : list;
    }

}