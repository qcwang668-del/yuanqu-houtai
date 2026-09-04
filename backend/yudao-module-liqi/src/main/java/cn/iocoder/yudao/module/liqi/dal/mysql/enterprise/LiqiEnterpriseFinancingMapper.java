package cn.iocoder.yudao.module.liqi.dal.mysql.enterprise;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseFinancingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 力企 - 企业库 · 融资信息 Mapper
 */
@Mapper
public interface LiqiEnterpriseFinancingMapper extends BaseMapperX<LiqiEnterpriseFinancingDO> {

    /** 按企业编号查询融资记录，融资时间倒序（最新在前） */
    default List<LiqiEnterpriseFinancingDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(new LambdaQueryWrapperX<LiqiEnterpriseFinancingDO>()
                .eq(LiqiEnterpriseFinancingDO::getEnterpriseId, enterpriseId)
                .orderByDesc(LiqiEnterpriseFinancingDO::getRzTime));
    }

    default void deleteByEnterpriseId(Long enterpriseId) {
        delete(new LambdaQueryWrapperX<LiqiEnterpriseFinancingDO>()
                .eq(LiqiEnterpriseFinancingDO::getEnterpriseId, enterpriseId));
    }

}
