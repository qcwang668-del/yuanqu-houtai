package cn.iocoder.yudao.module.liqi.service.parkseat;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo.*;
import cn.iocoder.yudao.module.liqi.dal.dataobject.appuser.LiqiAppUserDO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.park.LiqiParkDO;
import cn.iocoder.yudao.module.liqi.dal.dataobject.parkseat.*;
import cn.iocoder.yudao.module.liqi.dal.mysql.park.LiqiParkMapper;
import cn.iocoder.yudao.module.liqi.dal.mysql.parkseat.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.liqi.enums.ErrorCodeConstants.*;

/** 力企 - 园区会员席位 Service 实现 */
@Service
@Validated
public class LiqiParkSeatServiceImpl implements LiqiParkSeatService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ORDER_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private LiqiParkMapper parkMapper;
    @Resource
    private ParkSeatBatchMapper batchMapper;
    @Resource
    private ParkMemberMapper memberMapper;
    @Resource
    private MemberPlanMapper planMapper;
    @Resource
    private ParkSeatLogMapper logMapper;
    @Resource
    private ParkSeatOrderMapper orderMapper;
    @Resource
    private ParkSeatAccountMapper accountMapper;

    @Override
    public ParkSeatOverviewRespVO getOverview(Long parkId) {
        LiqiParkDO park = validatePark(parkId);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<ParkSeatBatchDO> batches = batchMapper.selectByParkId(parkId);
        List<ParkSeatBatchDO> validBatches = batches.stream()
                .filter(b -> b.getStatus() != null && b.getStatus() == 0
                        && b.getExpireDate() != null && !b.getExpireDate().isBefore(today))
                .collect(Collectors.toList());
        int totalQty = validBatches.stream().mapToInt(b -> b.getQty() == null ? 0 : b.getQty()).sum();
        int usedQty = memberMapper.countActiveByParkId(parkId, now).intValue();

        ParkSeatOverviewRespVO resp = new ParkSeatOverviewRespVO();
        resp.setParkId(parkId);
        resp.setParkName(park.getParkName());
        resp.setTotalQty(totalQty);
        resp.setUsedQty(usedQty);
        resp.setRemainingQty(Math.max(0, totalQty - usedQty));
        resp.setExpiringQty(memberMapper.countExpiring(parkId, now, now.plusDays(30)).intValue());
        resp.setCompanyChangedQty(countCompanyChanged(parkId, now));
        resp.setBatchCount(validBatches.size());
        resp.setBatches(validBatches.stream().map(b -> {
            ParkSeatOverviewRespVO.BatchVO vo = BeanUtils.toBean(b, ParkSeatOverviewRespVO.BatchVO.class);
            vo.setRemainingQty(Math.max(0, (b.getQty() == null ? 0 : b.getQty())
                    - (b.getUsedQty() == null ? 0 : b.getUsedQty())));
            return vo;
        }).collect(Collectors.toList()));
        return resp;
    }

    @Override
    public PageResult<ParkSeatAccountRespVO> getAccountPage(ParkSeatAccountPageReqVO reqVO) {
        LiqiParkDO park = validatePark(reqVO.getParkId());
        PageResult<LiqiAppUserDO> page = accountMapper.selectAccountPage(reqVO);
        List<LiqiAppUserDO> users = page.getList();
        Map<Long, ParkMemberDO> memberMap = getLatestMemberMap(
                users.stream().map(LiqiAppUserDO::getId).collect(Collectors.toList()));
        LocalDateTime now = LocalDateTime.now();
        List<ParkSeatAccountRespVO> list = users.stream().map(user -> {
            ParkSeatAccountRespVO vo = new ParkSeatAccountRespVO();
            vo.setUserId(user.getId());
            vo.setUserName(user.getUserName());
            vo.setPhone(user.getPhone());
            vo.setCompany(user.getMaintainEnterprise());
            vo.setParkId(park.getId());
            vo.setParkName(park.getParkName());
            ParkMemberDO member = memberMap.get(user.getId());
            if (member == null) {
                vo.setMemberStatus("NONE");
            } else {
                vo.setMemberStatus(member.getExpireTime() != null && member.getExpireTime().isAfter(now)
                        ? "ACTIVE" : "EXPIRED");
                vo.setMemberPlanName(member.getPlanName());
                vo.setMemberExpireTime(member.getExpireTime());
                vo.setMemberPrice(member.getPrice());
                vo.setCompanyChanged(!Objects.equals(member.getCompany(), user.getMaintainEnterprise()));
            }
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public List<MemberPlanRespVO> getPlanList() {
        return BeanUtils.toBean(planMapper.selectEnabledList(), MemberPlanRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParkSeatAssignRespVO assign(ParkSeatAssignReqVO reqVO) {
        LiqiParkDO park = validatePark(reqVO.getParkId());
        LiqiAppUserDO user = accountMapper.selectById(reqVO.getUserId());
        if (user == null) {
            throw exception(PARK_SEAT_ACCOUNT_NOT_EXISTS);
        }
        if (!Objects.equals(user.getParkId(), park.getId())) {
            throw exception(PARK_SEAT_ACCOUNT_NOT_IN_PARK);
        }
        LocalDateTime now = LocalDateTime.now();
        ParkMemberDO activeMember = memberMapper.selectActiveByUserId(user.getId(), now);
        if (activeMember != null) {
            throw exception(PARK_SEAT_MEMBER_EXISTS, activeMember.getPlanName(),
                    formatDate(activeMember.getExpireTime()));
        }
        MemberPlanDO plan = planMapper.selectByCode(reqVO.getPlanCode());
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 0) {
            throw exception(PARK_SEAT_PLAN_NOT_EXISTS);
        }
        ParkSeatBatchDO batch = batchMapper.selectAvailableBatch(reqVO.getParkId(), LocalDate.now());
        if (batch == null) {
            throw exception(PARK_SEAT_NOT_ENOUGH);
        }

        int days = plan.getDurationDays() == null ? 365 : plan.getDurationDays();
        LocalDateTime expireTime = now.plusDays(days);
        ParkMemberDO member = ParkMemberDO.builder()
                .parkId(park.getId())
                .userId(user.getId())
                .phone(user.getPhone())
                .userName(user.getUserName())
                .company(user.getMaintainEnterprise())
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                .price(plan.getPrice())
                .startTime(now)
                .expireTime(expireTime)
                .source("PARK_SEAT")
                .batchId(batch.getId())
                .status(0)
                .build();
        memberMapper.insert(member);

        batch.setUsedQty((batch.getUsedQty() == null ? 0 : batch.getUsedQty()) + 1);
        batchMapper.updateById(batch);

        ParkSeatLogDO log = ParkSeatLogDO.builder()
                .parkId(park.getId())
                .batchId(batch.getId())
                .userId(user.getId())
                .phone(user.getPhone())
                .userName(user.getUserName())
                .company(user.getMaintainEnterprise())
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                .action("ASSIGN")
                .result("SUCCESS")
                .remark("园区指派，立即生效，有效期 " + days + " 天")
                .operator("园区运营")
                .build();
        logMapper.insert(log);

        ParkSeatAssignRespVO resp = new ParkSeatAssignRespVO();
        resp.setMemberId(member.getId());
        resp.setPlanName(plan.getPlanName());
        resp.setStartTime(now);
        resp.setExpireTime(expireTime);
        int totalQty = getOverview(park.getId()).getTotalQty();
        resp.setRemainingQty(Math.max(0, totalQty - memberMapper.countActiveByParkId(park.getId(), now).intValue()));
        return resp;
    }

    @Override
    public PageResult<ParkSeatLogRespVO> getLogPage(ParkSeatLogPageReqVO reqVO) {
        PageResult<ParkSeatLogDO> page = logMapper.selectPage(reqVO);
        return BeanUtils.toBean(page, ParkSeatLogRespVO.class);
    }

    @Override
    public PageResult<ParkSeatOrderRespVO> getOrderPage(ParkSeatOrderPageReqVO reqVO) {
        PageResult<ParkSeatOrderDO> page = orderMapper.selectPage(reqVO);
        return BeanUtils.toBean(page, ParkSeatOrderRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParkSeatOrderRespVO createOrder(ParkSeatOrderCreateReqVO reqVO) {
        LiqiParkDO park = validatePark(reqVO.getParkId());
        if (!"PURCHASE".equals(reqVO.getOrderType()) && !"RENEW".equals(reqVO.getOrderType())) {
            throw exception(PARK_SEAT_ORDER_TYPE_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        BigDecimal unitPrice = reqVO.getUnitPrice() == null ? new BigDecimal("800") : reqVO.getUnitPrice();
        int qty = reqVO.getQty();
        String orderNo = "PSO-" + ORDER_FMT.format(now) + "-" + String.format("%03d", new Random().nextInt(1000));
        ParkSeatOrderDO order = ParkSeatOrderDO.builder()
                .parkId(park.getId())
                .orderNo(orderNo)
                .orderType(reqVO.getOrderType())
                .qty(qty)
                .unitPrice(unitPrice)
                .amount(unitPrice.multiply(new BigDecimal(qty)))
                .status(1)
                .payTime(now)
                .remark("PURCHASE".equals(reqVO.getOrderType()) ? "园区增购席位" : "园区续费席位")
                .build();
        orderMapper.insert(order);

        LocalDate today = LocalDate.now();
        ParkSeatBatchDO batch = ParkSeatBatchDO.builder()
                .parkId(park.getId())
                .batchNo("SEAT-" + park.getId() + "-" + ORDER_FMT.format(now) + "-" + String.format("%03d", new Random().nextInt(1000)))
                .source("PURCHASE".equals(reqVO.getOrderType()) ? "增购" : "续费")
                .qty(qty)
                .usedQty(0)
                .startDate(today)
                .expireDate(today.plusDays(365))
                .status(0)
                .build();
        batchMapper.insert(batch);
        return BeanUtils.toBean(order, ParkSeatOrderRespVO.class);
    }

    private LiqiParkDO validatePark(Long parkId) {
        LiqiParkDO park = parkId == null ? null : parkMapper.selectById(parkId);
        if (park == null) {
            throw exception(PARK_NOT_EXISTS);
        }
        return park;
    }

    private Map<Long, ParkMemberDO> getLatestMemberMap(List<Long> userIds) {
        Map<Long, ParkMemberDO> map = new HashMap<>();
        memberMapper.selectLatestByUserIds(userIds)
                .forEach(member -> map.putIfAbsent(member.getUserId(), member));
        return map;
    }

    private int countCompanyChanged(Long parkId, LocalDateTime now) {
        List<ParkMemberDO> members = memberMapper.selectActiveByParkId(parkId, now);
        if (members.isEmpty()) {
            return 0;
        }
        List<Long> userIds = members.stream().map(ParkMemberDO::getUserId).distinct().collect(Collectors.toList());
        List<LiqiAppUserDO> users = accountMapper.selectBatchIds(userIds);
        Map<Long, String> companyMap = users.stream()
                .collect(Collectors.toMap(LiqiAppUserDO::getId,
                        u -> u.getMaintainEnterprise() == null ? "" : u.getMaintainEnterprise(), (a, b) -> a));
        return (int) members.stream()
                .filter(m -> !Objects.equals(m.getCompany() == null ? "" : m.getCompany(),
                        companyMap.getOrDefault(m.getUserId(), "")))
                .count();
    }

    private String formatDate(LocalDateTime time) {
        return time == null ? "-" : DATE_FMT.format(time);
    }
}
