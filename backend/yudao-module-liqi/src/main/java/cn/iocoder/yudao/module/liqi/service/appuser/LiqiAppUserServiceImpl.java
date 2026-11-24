package cn.iocoder.yudao.module.liqi.service.appuser;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.liqi.controller.admin.appuser.vo.AppUserPageReqVO;
import cn.iocoder.yudao.module.liqi.controller.admin.appuser.vo.AppUserRespVO;
import cn.iocoder.yudao.module.liqi.controller.admin.appuser.vo.AppUserSaveReqVO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.appuser.LiqiAppUserDO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.park.LiqiParkDO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat.ParkMemberDO;
import cn.iocoder.yudao.module.liqi.dal.mysql.appuser.LiqiAppUserMapper;
import cn.iocoder.yudao.module.liqi.dal.mysql.park.LiqiParkMapper;
import cn.iocoder.yudao.module.liqi.dal.mysql.parkseat.ParkMemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.liqi.enums.ErrorCodeConstants.APP_USER_NOT_EXISTS;

/**
 * 力企 - APP 用户 Service 实现类
 */
@Service
@Validated
public class LiqiAppUserServiceImpl implements LiqiAppUserService {

    @Resource
    private LiqiAppUserMapper appUserMapper;
    @Resource
    private ParkMemberMapper parkMemberMapper;
    @Resource
    private LiqiParkMapper parkMapper;

    @Override
    public Long createAppUser(AppUserSaveReqVO createReqVO) {
        LiqiAppUserDO appUser = BeanUtils.toBean(createReqVO, LiqiAppUserDO.class);
        appUserMapper.insert(appUser);
        return appUser.getId();
    }

    @Override
    public void updateAppUser(AppUserSaveReqVO updateReqVO) {
        validateAppUserExists(updateReqVO.getId());
        LiqiAppUserDO updateObj = BeanUtils.toBean(updateReqVO, LiqiAppUserDO.class);
        appUserMapper.updateById(updateObj);
    }

    @Override
    public void deleteAppUser(Long id) {
        validateAppUserExists(id);
        appUserMapper.deleteById(id);
    }

    private void validateAppUserExists(Long id) {
        if (appUserMapper.selectById(id) == null) {
            throw exception(APP_USER_NOT_EXISTS);
        }
    }

    @Override
    public LiqiAppUserDO getAppUser(Long id) {
        return appUserMapper.selectById(id);
    }

    @Override
    public PageResult<LiqiAppUserDO> getAppUserPage(AppUserPageReqVO pageReqVO) {
        return appUserMapper.selectPage(pageReqVO);
    }

    @Override
    public void fillMemberInfo(List<AppUserRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream().map(AppUserRespVO::getId).filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, ParkMemberDO> memberMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            parkMemberMapper.selectLatestByUserIds(userIds)
                    .forEach(member -> memberMap.putIfAbsent(member.getUserId(), member));
        }
        Set<Long> parkIds = list.stream().map(AppUserRespVO::getParkId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> parkNameMap = parkIds.isEmpty() ? Collections.emptyMap()
                : parkMapper.selectBatchIds(parkIds).stream()
                .collect(Collectors.toMap(LiqiParkDO::getId, LiqiParkDO::getParkName, (a, b) -> a));
        LocalDateTime now = LocalDateTime.now();
        for (AppUserRespVO vo : list) {
            vo.setParkName(parkNameMap.get(vo.getParkId()));
            ParkMemberDO member = memberMap.get(vo.getId());
            if (member == null) {
                vo.setMemberStatus("NONE");
                continue;
            }
            vo.setMemberStatus(member.getExpireTime() != null && member.getExpireTime().isAfter(now)
                    ? "ACTIVE" : "EXPIRED");
            vo.setMemberPlanName(member.getPlanName());
            vo.setMemberExpireTime(member.getExpireTime());
        }
    }

}
