package cn.iocoder.yudao.module.liqi.message.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.liqi.message.controller.admin.vo.ParkPushCandidateVO;
import cn.iocoder.yudao.module.liqi.message.controller.admin.vo.ParkPushFilterVO;
import cn.iocoder.yudao.module.liqi.message.controller.admin.vo.UserMessagePageReqVO;
import cn.iocoder.yudao.module.liqi.message.dal.dataobject.LiqiUserMessageDO;

import java.util.List;

/**
 * 力企 - C 端站内消息 Service（政策精准推送 / 临期提醒 / 系统通知）
 *
 * <p>消息来源 source：external=外部采集政策（自动按企业画像推）；
 * park=园区发布政策（园区运营手动筛选企业推）。</p>
 */
public interface LiqiUserMessageService {

    // ---- C 端 ----
    /** 我的消息分页；source=external/park 时按来源过滤，空=全部 */
    PageResult<LiqiUserMessageDO> pageMyMessages(Long userId, Integer pageNo, Integer pageSize, String source);

    Long unreadCount(Long userId);

    void markRead(Long userId);

    // ---- 运营定向推送 ----
    /**
     * 运营选一条政策定向推送（自动按政策 ID 判断 source：L 开头=园区，其余=外部）。
     *
     * @param target all=所有已绑定企业会员；match=画像与该政策匹配的会员
     */
    int pushByPolicy(String policyId, String target);

    /** 园区运营：预览某条园区政策可推送的候选企业名单（本园区绑定企业 + 画像匹配结果）。 */
    List<ParkPushCandidateVO> candidatesOfParkPolicy(String policyId);

    /** 园区运营：把一条园区政策推送给勾选的会员（已推过的跳过）。 */
    int pushParkPolicyToUsers(String policyId, List<Long> userIds);

    /**
     * 园区发布项目：按企业画像圈选客户对象，返回候选名单。
     *
     * <p>圈选范围 = 园区客户管理（liqi_member）中归属本园区的企业；再按 filter 做画像过滤。
     * 园区客户需已在小程序注册绑定才有 userId 可接收消息，未绑定的企业以
     * {@code pushable=false} 返回并说明原因，便于运营看到转化缺口。</p>
     */
    List<ParkPushCandidateVO> candidatesOfParkPolicyByFilter(String policyId, ParkPushFilterVO filter);

    /** 园区发布项目：按企业画像统计命中的客户数（供发布页实时回显「共 N 家」） */
    Long countCandidatesByFilter(String policyId, ParkPushFilterVO filter);

    /** 管理后台：推送记录分页 */
    PageResult<LiqiUserMessageDO> getAdminPage(UserMessagePageReqVO reqVO);

    // ---- 定时任务 ----
    /**
     * 自动"政策找人"：按会员绑定企业画像，把近 7 天新政策匹配推送（source=external，已去重）。
     * 不再依赖订阅偏好（订阅能力已下线）。
     */
    int autoMatchPush();

}
