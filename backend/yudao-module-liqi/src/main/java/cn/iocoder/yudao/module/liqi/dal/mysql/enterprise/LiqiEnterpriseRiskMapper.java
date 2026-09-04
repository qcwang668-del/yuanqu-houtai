package cn.iocoder.yudao.module.liqi.dal.mysql.enterprise;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseRiskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 力企 - 企业库 · 经营风险 Mapper
 */
@Mapper
public interface LiqiEnterpriseRiskMapper extends BaseMapperX<LiqiEnterpriseRiskDO> {

    default List<LiqiEnterpriseRiskDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(new LambdaQueryWrapperX<LiqiEnterpriseRiskDO>()
                .eq(LiqiEnterpriseRiskDO::getEnterpriseId, enterpriseId));
    }

    default void deleteByEnterpriseId(Long enterpriseId) {
        delete(new LambdaQueryWrapperX<LiqiEnterpriseRiskDO>()
                .eq(LiqiEnterpriseRiskDO::getEnterpriseId, enterpriseId));
    }

}
