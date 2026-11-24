package cn.iocoder.yudao.module.liqi.dal.mysql.parkseat;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo.ParkSeatAccountPageReqVO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.appuser.LiqiAppUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkSeatAccountMapper extends BaseMapperX<LiqiAppUserDO> {

    default PageResult<LiqiAppUserDO> selectAccountPage(ParkSeatAccountPageReqVO reqVO) {
        String keyword = reqVO.getKeyword();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        return selectPage(reqVO, new LambdaQueryWrapperX<LiqiAppUserDO>()
                .eq(LiqiAppUserDO::getParkId, reqVO.getParkId())
                .and(hasKeyword, wrapper -> wrapper
                        .like(LiqiAppUserDO::getUserName, keyword)
                        .or().like(LiqiAppUserDO::getPhone, keyword)
                        .or().like(LiqiAppUserDO::getMaintainEnterprise, keyword))
                .orderByAsc(LiqiAppUserDO::getId));
    }
}
