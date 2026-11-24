package cn.iocoder.yudao.module.liqi.controller.admin.parkseat;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.liqi.controller.admin.park.vo.ParkRespVO;
import cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo.*;
import cn.iocoder.yudao.module.liqi.service.park.LiqiParkService;
import cn.iocoder.yudao.module.liqi.service.parkseat.LiqiParkSeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 园区会员席位")
@RestController
@RequestMapping("/liqi/park-seat")
@Validated
public class LiqiParkSeatController {

    @Resource
    private LiqiParkSeatService parkSeatService;
    @Resource
    private LiqiParkService parkService;

    @GetMapping("/park/list")
    @Operation(summary = "园区下拉列表")
    @PreAuthorize("@ss.hasPermission('liqi:park-seat:query')")
    public CommonResult<List<ParkRespVO>> getParkList() {
        return success(BeanUtils.toBean(parkService.getEnabledParkList(), ParkRespVO.class));
    }

    @GetMapping("/overview")
    @Operation(summary = "园区席位总览")
    @Parameter(name = "parkId", description = "园区编号", required = true)
    @PreAuthorize("@ss.hasPermission('liqi:park-seat:query')")
    public CommonResult<ParkSeatOverviewRespVO> getOverview(@RequestParam("parkId") Long parkId) {
        return success(parkSeatService.getOverview(parkId));
    }

    @GetMapping("/account/page")
    @Operation(summary = "本园区可指派账号分页")
    @PreAuthorize("@ss.hasPermission('liqi:park-seat:query')")
    public CommonResult<PageResult<ParkSeatAccountRespVO>> getAccountPage(@Valid ParkSeatAccountPageReqVO reqVO) {
        return success(parkSeatService.getAccountPage(reqVO));
    }

    @GetMapping("/plan/list")
    @Operation(summary = "可指派会员套餐")
    @PreAuthorize("@ss.hasPermission('liqi:park-seat:query')")
    public CommonResult<List<MemberPlanRespVO>> getPlanList() {
        return success(parkSeatService.getPlanList());
    }

    @PostMapping("/assign")
    @Operation(summary = "指派园区会员（立即生效）")
    @PreAuthorize("@ss.hasPermission('liqi:park-seat:query')")
    public CommonResult<ParkSeatAssignRespVO> assign(@Valid @RequestBody ParkSeatAssignReqVO reqVO) {
        return success(parkSeatService.assign(reqVO));
    }

    @GetMapping("/log/page")
    @Operation(summary = "指派记录分页")
    @PreAuthorize("@ss.hasPermission('liqi:park-seat:query')")
    public CommonResult<PageResult<ParkSeatLogRespVO>> getLogPage(@Valid ParkSeatLogPageReqVO reqVO) {
        return success(parkSeatService.getLogPage(reqVO));
    }

    @GetMapping("/order/page")
    @Operation(summary = "增购/续费记录分页")
    @PreAuthorize("@ss.hasPermission('liqi:park-seat:query')")
    public CommonResult<PageResult<ParkSeatOrderRespVO>> getOrderPage(@Valid ParkSeatOrderPageReqVO reqVO) {
        return success(parkSeatService.getOrderPage(reqVO));
    }

    @PostMapping("/order/create")
    @Operation(summary = "园区增购/续费（支付完成即生效）")
    @PreAuthorize("@ss.hasPermission('liqi:park-seat:query')")
    public CommonResult<ParkSeatOrderRespVO> createOrder(@Valid @RequestBody ParkSeatOrderCreateReqVO reqVO) {
        return success(parkSeatService.createOrder(reqVO));
    }
}
