package cn.iocoder.yudao.module.liqi.dal.mysql.parkseat;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat.ParkMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface ParkMemberMapper extends BaseMapperX<ParkMemberDO> {

    default ParkMemberDO selectActiveByUserId(Long userId, LocalDateTime now) {
        List<ParkMemberDO> list = selectActiveByUserIds(Collections.singletonList(userId), now);
        return list.isEmpty() ? null : list.get(0);
    }

    default List<ParkMemberDO> selectActiveByUserIds(Collection<Long> userIds, LocalDateTime now) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ParkMemberDO>()
                .in(ParkMemberDO::getUserId, userIds)
                .gt(ParkMemberDO::getExpireTime, now)
                .orderByDesc(ParkMemberDO::getExpireTime));
    }

    default List<ParkMemberDO> selectLatestByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ParkMemberDO>()
                .in(ParkMemberDO::getUserId, userIds)
                .orderByDesc(ParkMemberDO::getExpireTime));
    }

    default List<ParkMemberDO> selectActiveByParkId(Long parkId, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<ParkMemberDO>()
                .eq(ParkMemberDO::getParkId, parkId)
                .gt(ParkMemberDO::getExpireTime, now)
                .orderByDesc(ParkMemberDO::getExpireTime));
    }

    default Long countActiveByParkId(Long parkId, LocalDateTime now) {
        return selectCount(new LambdaQueryWrapperX<ParkMemberDO>()
                .eq(ParkMemberDO::getParkId, parkId)
                .gt(ParkMemberDO::getExpireTime, now));
    }

    default Long countExpiring(Long parkId, LocalDateTime now, LocalDateTime deadline) {
        return selectCount(new LambdaQueryWrapperX<ParkMemberDO>()
                .eq(ParkMemberDO::getParkId, parkId)
                .gt(ParkMemberDO::getExpireTime, now)
                .le(ParkMemberDO::getExpireTime, deadline));
    }
}
