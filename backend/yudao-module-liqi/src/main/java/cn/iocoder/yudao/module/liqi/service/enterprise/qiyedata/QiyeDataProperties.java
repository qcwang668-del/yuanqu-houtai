package cn.iocoder.yudao.module.liqi.service.enterprise.qiyedata;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 企业数据平台（qiyedata）开放接口配置。
 *
 * <p>凭据由启动进程的环境变量注入（QIYEDATA_APP_KEY / QIYEDATA_APP_SECRET），禁止硬编码进仓库。
 * 该接口仅授权本项目使用：appKey/appSecret 不得外泄，也不得由本系统代理给外部项目调用。</p>
 */
@Data
@Component
public class QiyeDataProperties {

    /** 接口基础地址（不含 /app-api 之后的业务路径） */
    @Value("${liqi.qiyedata.base-url:}")
    private String baseUrl;

    /** 企业基本信息路径 */
    @Value("${liqi.qiyedata.base-info-path:/api/business/qiyedata/qiye-base-info}")
    private String baseInfoPath;

    /** 平台分配的 appKey（env: QIYEDATA_APP_KEY） */
    @Value("${liqi.qiyedata.app-key:}")
    private String appKey;

    /** 平台分配的 appSecret（env: QIYEDATA_APP_SECRET），仅参与本地签名计算，不随请求发送 */
    @Value("${liqi.qiyedata.app-secret:}")
    private String appSecret;

    /** 租户编号（请求头 tenant-id），为空则不发送 */
    @Value("${liqi.qiyedata.tenant-id:}")
    private String tenantId;

    /** 请求超时（毫秒） */
    @Value("${liqi.qiyedata.timeout:20000}")
    private Integer timeout;

    /** 配置是否完整（缺少任一必填项则视为未开通） */
    public boolean available() {
        return isNotBlank(baseUrl) && isNotBlank(appKey) && isNotBlank(appSecret);
    }

    private static boolean isNotBlank(String v) {
        return v != null && !v.trim().isEmpty();
    }

}
