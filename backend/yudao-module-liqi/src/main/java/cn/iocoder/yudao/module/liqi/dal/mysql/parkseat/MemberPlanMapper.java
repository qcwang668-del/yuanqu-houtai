package cn.iocoder.yudao.module.liqi.dal.mysql.parkseat;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat.MemberPlanDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberPlanMapper extends BaseMapperX<MemberPlanDO> {

    default List<MemberPlanDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<MemberPlanDO>()
                .eq(MemberPlanDO::getStatus, 0)
                .orderByAsc(MemberPlanDO::getSort));
    }

    default MemberPlanDO selectByCode(String planCode) {
        return selectOne(new LambdaQueryWrapperX<MemberPlanDO>()
                .eq(MemberPlanDO::getPlanCode, planCode)
                .last("LIMIT 1"));
    }
}
