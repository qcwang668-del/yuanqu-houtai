package cn.iocoder.yudao.module.liqi.agent.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.agent.dal.dataobject.LiqiAgentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 力企 - 智能体 Mapper
 */
@Mapper
public interface LiqiAgentMapper extends BaseMapperX<LiqiAgentDO> {

    default List<LiqiAgentDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<LiqiAgentDO>()
                .eq(LiqiAgentDO::getStatus, 0)
                .orderByAsc(LiqiAgentDO::getSort)
                .orderByAsc(LiqiAgentDO::getId));
    }

}
