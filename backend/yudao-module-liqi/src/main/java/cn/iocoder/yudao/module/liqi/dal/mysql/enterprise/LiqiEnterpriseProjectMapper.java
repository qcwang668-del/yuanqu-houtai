package cn.iocoder.yudao.module.liqi.dal.mysql.enterprise;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise.LiqiEnterpriseProjectDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 力企 - 企业库 · 项目申报 Mapper
 */
@Mapper
public interface LiqiEnterpriseProjectMapper extends BaseMapperX<LiqiEnterpriseProjectDO> {

    default List<LiqiEnterpriseProjectDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(new LambdaQueryWrapperX<LiqiEnterpriseProjectDO>()
                .eq(LiqiEnterpriseProjectDO::getEnterpriseId, enterpriseId));
    }

    default void deleteByEnterpriseId(Long enterpriseId) {
        delete(new LambdaQueryWrapperX<LiqiEnterpriseProjectDO>()
                .eq(LiqiEnterpriseProjectDO::getEnterpriseId, enterpriseId));
    }

}
