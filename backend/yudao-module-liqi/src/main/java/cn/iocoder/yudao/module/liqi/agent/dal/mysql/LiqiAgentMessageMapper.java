package cn.iocoder.yudao.module.liqi.agent.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.agent.dal.dataobject.LiqiAgentMessageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 力企 - 智能体消息 Mapper
 */
@Mapper
public interface LiqiAgentMessageMapper extends BaseMapperX<LiqiAgentMessageDO> {

    default List<LiqiAgentMessageDO> selectListByConversationId(Long conversationId) {
        return selectList(new LambdaQueryWrapperX<LiqiAgentMessageDO>()
                .eq(LiqiAgentMessageDO::getConversationId, conversationId)
                .orderByAsc(LiqiAgentMessageDO::getId));
    }

}
