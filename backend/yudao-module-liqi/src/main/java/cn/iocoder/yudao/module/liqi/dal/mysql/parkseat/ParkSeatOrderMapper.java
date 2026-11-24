package cn.iocoder.yudao.module.liqi.dal.mysql.parkseat;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo.ParkSeatOrderPageReqVO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat.ParkSeatOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkSeatOrderMapper extends BaseMapperX<ParkSeatOrderDO> {

    default PageResult<ParkSeatOrderDO> selectPage(ParkSeatOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ParkSeatOrderDO>()
                .eq(ParkSeatOrderDO::getParkId, reqVO.getParkId())
                .orderByDesc(ParkSeatOrderDO::getId));
    }
}
