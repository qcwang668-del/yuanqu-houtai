package cn.iocoder.yudao.module.liqi.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 力企模块错误码（区间 1-099-000-000 ~ 1-099-999-999）
 */
public interface ErrorCodeConstants {

    ErrorCode APP_USER_NOT_EXISTS = new ErrorCode(1_099_001_001, "APP 用户不存在");

    // ========== 智能体广场 1-099-002-xxx ==========
    ErrorCode AGENT_NOT_EXISTS = new ErrorCode(1_099_002_001, "智能体不存在或已关闭");
    ErrorCode AGENT_CONVERSATION_NOT_EXISTS = new ErrorCode(1_099_002_002, "会话不存在");

}
