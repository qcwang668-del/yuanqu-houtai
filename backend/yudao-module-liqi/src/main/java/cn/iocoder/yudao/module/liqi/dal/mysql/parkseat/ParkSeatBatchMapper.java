package cn.iocoder.yudao.module.liqi.dal.mysql.parkseat;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat.ParkSeatBatchDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ParkSeatBatchMapper extends BaseMapperX<ParkSeatBatchDO> {

    default List<ParkSeatBatchDO> selectByParkId(Long parkId) {
        return selectList(new LambdaQueryWrapperX<ParkSeatBatchDO>()
                .eq(ParkSeatBatchDO::getParkId, parkId)
                .orderByAsc(ParkSeatBatchDO::getExpireDate));
    }

    default List<ParkSeatBatchDO> selectValidByParkId(Long parkId, LocalDate today) {
        return selectList(new LambdaQueryWrapperX<ParkSeatBatchDO>()
                .eq(ParkSeatBatchDO::getParkId, parkId)
                .eq(ParkSeatBatchDO::getStatus, 0)
                .ge(ParkSeatBatchDO::getExpireDate, today)
                .orderByAsc(ParkSeatBatchDO::getExpireDate));
    }

    default ParkSeatBatchDO selectAvailableBatch(Long parkId, LocalDate today) {
        List<ParkSeatBatchDO> list = selectList(new LambdaQueryWrapperX<ParkSeatBatchDO>()
                .eq(ParkSeatBatchDO::getParkId, parkId)
                .eq(ParkSeatBatchDO::getStatus, 0)
                .ge(ParkSeatBatchDO::getExpireDate, today)
                .apply("used_qty < qty")
                .orderByAsc(ParkSeatBatchDO::getExpireDate));
        return list.isEmpty() ? null : list.get(0);
    }
}
