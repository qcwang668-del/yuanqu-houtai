package cn.iocoder.yudao.module.liqi.dal.mysql.enterprise;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.controller.admin.enterprise.vo.EnterprisePageReqVO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
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

}
