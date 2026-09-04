package cn.iocoder.yudao.module.liqi.dal.mysql.enterprise;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseContactDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 力企 - 企业库 · 联系方式 Mapper
 */
@Mapper
public interface LiqiEnterpriseContactMapper extends BaseMapperX<LiqiEnterpriseContactDO> {

    default List<LiqiEnterpriseContactDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(new LambdaQueryWrapperX<LiqiEnterpriseContactDO>()
                .eq(LiqiEnterpriseContactDO::getEnterpriseId, enterpriseId));
    }

    default void deleteByEnterpriseId(Long enterpriseId) {
        delete(new LambdaQueryWrapperX<LiqiEnterpriseContactDO>()
                .eq(LiqiEnterpriseContactDO::getEnterpriseId, enterpriseId));
    }

}
