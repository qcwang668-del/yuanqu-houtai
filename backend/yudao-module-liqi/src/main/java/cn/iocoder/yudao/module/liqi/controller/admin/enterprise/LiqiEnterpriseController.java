package cn.iocoder.yudao.module.liqi.controller.admin.enterprise;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.liqi.controller.admin.enterprise.vo.EnterprisePageReqVO;
import cn.iocoder.yudao.module.liqi.controller.admin.enterprise.vo.EnterpriseRespVO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseDO;
import cn.iocoder.yudao.module.liqi.service.enterprise.LiqiEnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 企业库（深圳湾生态园）")
@RestController
@RequestMapping("/liqi/enterprise")
@Validated
public class LiqiEnterpriseController {

    @Resource
    private LiqiEnterpriseService enterpriseService;

    @GetMapping("/page")
    @Operation(summary = "获得企业分页")
    @PreAuthorize("@ss.hasPermission('liqi:enterprise:query')")
    public CommonResult<PageResult<EnterpriseRespVO>> getEnterprisePage(@Valid EnterprisePageReqVO pageReqVO) {
        PageResult<LiqiEnterpriseDO> pageResult = enterpriseService.getEnterprisePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, EnterpriseRespVO.class));
    }

    @GetMapping("/stats")
    @Operation(summary = "企业统计（供园区大屏调用）")
    @PreAuthorize("@ss.hasPermission('liqi:enterprise:query')")
    public CommonResult<Map<String, Object>> getStats() {
        return success(enterpriseService.getEnterpriseStats());
    }

    @GetMapping("/get")
    @Operation(summary = "获得企业详情（含股东子表）")
    @PreAuthorize("@ss.hasPermission('liqi:enterprise:query')")
    public CommonResult<Map<String, Object>> getDetail(@RequestParam("id") Long id) {
        return success(enterpriseService.getEnterpriseDetail(id));
    }

    @GetMapping("/get-by-name")
    @Operation(summary = "按企业名称获得企业详情（供会员/线索等无企业外键的列表调用）")
    @PreAuthorize("@ss.hasPermission('liqi:enterprise:query')")
    public CommonResult<Map<String, Object>> getDetailByName(@RequestParam("enterpriseName") String enterpriseName) {
        return success(enterpriseService.getEnterpriseDetailByName(enterpriseName));
    }

    @PostMapping("/enrich")
    @Operation(summary = "按需实时调天眼查回填工商信息 + 股东")
    @PreAuthorize("@ss.hasPermission('liqi:enterprise:query')")
    public CommonResult<Map<String, Object>> enrich(@RequestParam("id") Long id) {
        return success(enterpriseService.enrich(id));
    }

    /**
     * 企业数据平台「企业基本信息」同步。
     *
     * <p><b>仅限本项目使用</b>：入参只接受企业库内部编号 id，entityId 由库内数据推导；
     * 不提供按 entityId 直查的透传能力，避免本接口被当作该第三方接口的对外代理。</p>
     */
    @PostMapping("/sync-base-info")
    @Operation(summary = "同步企业基本信息（企业数据平台，仅本项目内部使用）")
    @PreAuthorize("@ss.hasPermission('liqi:enterprise:query')")
    public CommonResult<Map<String, Object>> syncBaseInfo(@RequestParam("id") Long id) {
        return success(enterpriseService.syncBaseInfo(id));
    }

    /**
     * 批量补全企业基本信息（仅本项目内部使用）。
     *
     * <p>取库内 enrich_status=0 且有 entityId/信用代码的企业逐个补全，失败不中断整批；
     * 分多次调用即可断点续跑，interval 用于限流保护第三方接口。</p>
     */
    @PostMapping("/sync-base-info-batch")
    @Operation(summary = "批量补全企业基本信息（企业数据平台，仅本项目内部使用）")
    @PreAuthorize("@ss.hasPermission('liqi:enterprise:query')")
    public CommonResult<Map<String, Object>> syncBaseInfoBatch(
            @RequestParam(value = "limit", defaultValue = "100") Integer limit,
            @RequestParam(value = "interval", defaultValue = "200") Long interval) {
        return success(enterpriseService.syncBaseInfoBatch(limit, interval));
    }

    @GetMapping("/enrich-progress")
    @Operation(summary = "企业库补全进度统计")
    @PreAuthorize("@ss.hasPermission('liqi:enterprise:query')")
    public CommonResult<Map<String, Object>> getEnrichProgress() {
        return success(enterpriseService.getEnrichProgress());
    }

}
