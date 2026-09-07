package cn.iocoder.yudao.module.liqi.service.enterprise;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.liqi.controller.admin.enterprise.vo.EnterprisePageReqVO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseDO;

import java.util.Map;

/**
 * 力企 - 企业库 Service
 */
public interface LiqiEnterpriseService {

    PageResult<LiqiEnterpriseDO> getEnterprisePage(EnterprisePageReqVO pageReqVO);

    /** 企业统计（供园区大屏调用）：total 总数、industries 行业分布、topList 榜单 */
    Map<String, Object> getEnterpriseStats();

    /** 企业详情：主表全字段 + 股东子表 */
    Map<String, Object> getEnterpriseDetail(Long id);

    /**
     * 按企业名称查企业详情。
     *
     * <p>会员/会员线索/获批动态等业务表与企业库无外键关联，其行编号并非企业库编号，
     * 只能按企业名称回查企业库，否则会取到 id 相同的另一家企业（详情与融资数据错乱）。</p>
     *
     * @param enterpriseName 企业名称（工商全称）
     * @return 与 {@link #getEnterpriseDetail(Long)} 同结构；企业库无此名称时 enterprise 为 null
     */
    Map<String, Object> getEnterpriseDetailByName(String enterpriseName);

    /**
     * 按需实时调天眼查回填企业工商信息 + 股东，写入企业库并返回最新详情。
     * 天眼查仅认工商登记全称/统一社会信用代码；虚构名/简称查无结果时 enrichStatus=2。
     */
    Map<String, Object> enrich(Long id);

    /**
     * 调企业数据平台「企业基本信息」接口（/app-api/business/qiyedata/qiye-base-info）回填企业库。
     *
     * <p>入参只接受企业库内部编号，entityId 由库内 entity_id / credit_code 推导，
     * 避免本系统成为该接口的对外查询代理（接口仅授权本项目使用）。</p>
     *
     * @param id 企业库编号
     * @return code/synced/msg + 最新详情
     */
    Map<String, Object> syncBaseInfo(Long id);

    /**
     * 批量补全企业基本信息：取 enrich_status=0 且有 entityId/信用代码的企业，逐个调接口回填。
     *
     * <p>单次最多处理 limit 家，每家之间按 intervalMs 限流；失败不中断整批，
     * 已成功的记录状态改写后不会被再次命中，支持分多次断点续跑。</p>
     *
     * @param limit      本次最多处理数量
     * @param intervalMs 每次调用间隔毫秒（限流，避免打爆第三方接口）
     * @return 处理结果统计
     */
    Map<String, Object> syncBaseInfoBatch(int limit, long intervalMs);

    /** 企业库补全进度统计：总数/已补全/查无结果/待补全/可补全 */
    Map<String, Object> getEnrichProgress();

}
