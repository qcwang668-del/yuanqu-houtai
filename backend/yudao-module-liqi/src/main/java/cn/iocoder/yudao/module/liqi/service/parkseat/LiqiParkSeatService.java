package cn.iocoder.yudao.module.liqi.service.parkseat;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo.*;

import java.util.List;

/** 力企 - 园区会员席位 Service */
public interface LiqiParkSeatService {

    ParkSeatOverviewRespVO getOverview(Long parkId);

    PageResult<ParkSeatAccountRespVO> getAccountPage(ParkSeatAccountPageReqVO reqVO);

    List<MemberPlanRespVO> getPlanList();

    ParkSeatAssignRespVO assign(ParkSeatAssignReqVO reqVO);

    PageResult<ParkSeatLogRespVO> getLogPage(ParkSeatLogPageReqVO reqVO);

    PageResult<ParkSeatOrderRespVO> getOrderPage(ParkSeatOrderPageReqVO reqVO);

    ParkSeatOrderRespVO createOrder(ParkSeatOrderCreateReqVO reqVO);
}
