package cn.iocoder.yudao.module.liqi.dal.mysql.enterprise;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseIpDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 力企 - 企业库 · 知识产权 Mapper
 */
@Mapper
public interface LiqiEnterpriseIpMapper extends BaseMapperX<LiqiEnterpriseIpDO> {

    default List<LiqiEnterpriseIpDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(new LambdaQueryWrapperX<LiqiEnterpriseIpDO>()
                .eq(LiqiEnterpriseIpDO::getEnterpriseId, enterpriseId));
    }

    default void deleteByEnterpriseId(Long enterpriseId) {
        delete(new LambdaQueryWrapperX<LiqiEnterpriseIpDO>()
                .eq(LiqiEnterpriseIpDO::getEnterpriseId, enterpriseId));
    }

}
