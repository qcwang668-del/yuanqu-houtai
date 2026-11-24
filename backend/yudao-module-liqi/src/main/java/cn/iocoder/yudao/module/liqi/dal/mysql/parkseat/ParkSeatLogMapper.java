package cn.iocoder.yudao.module.liqi.dal.mysql.parkseat;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo.ParkSeatLogPageReqVO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat.ParkSeatLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkSeatLogMapper extends BaseMapperX<ParkSeatLogDO> {

    default PageResult<ParkSeatLogDO> selectPage(ParkSeatLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ParkSeatLogDO>()
                .eq(ParkSeatLogDO::getParkId, reqVO.getParkId())
                .likeIfPresent(ParkSeatLogDO::getPhone, reqVO.getKeyword())
                .orderByDesc(ParkSeatLogDO::getId));
    }
}
