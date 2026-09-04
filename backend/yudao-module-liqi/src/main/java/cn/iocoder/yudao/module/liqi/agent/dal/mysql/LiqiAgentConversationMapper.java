package cn.iocoder.yudao.module.liqi.agent.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.agent.dal.dataobject.LiqiAgentConversationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 力企 - 智能体会话 Mapper
 */
@Mapper
public interface LiqiAgentConversationMapper extends BaseMapperX<LiqiAgentConversationDO> {

    default List<LiqiAgentConversationDO> selectListByAgentAndUser(Long agentId, Long userId) {
        return selectList(new LambdaQueryWrapperX<LiqiAgentConversationDO>()
                .eqIfPresent(LiqiAgentConversationDO::getAgentId, agentId)
                .eq(LiqiAgentConversationDO::getUserId, userId)
                .orderByDesc(LiqiAgentConversationDO::getId));
    }

}
