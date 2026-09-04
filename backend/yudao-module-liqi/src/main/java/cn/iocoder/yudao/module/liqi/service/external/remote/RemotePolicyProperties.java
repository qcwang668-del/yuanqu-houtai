package cn.iocoder.yudao.module.liqi.service.external.remote;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 力企云 - 真实政策数据源（DaaS 政策库）配置。
 *
 * <p>对应 application.yaml 中 {@code liqi.policy.*} 配置段：
 * <pre>
 * liqi:
 *   policy:
 *     remote-enabled: true      # 打开后用 DaaS 真实政策替换 MockPolicyProvider
 *     province-code: 440000     # 广东省
 *     city-code: 440300         # 深圳市（H5 端默认城市）
 *     area-code:                # 区县，留空表示全市
 *     prefetch-pages: 8         # 预热页数，8 * 50 = 400 条兜底缓存
 *     list-cache-minutes: 30    # 列表缓存时长
 *     detail-cache-hours: 24    # 详情缓存时长
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "liqi.policy")
@Data
public class RemotePolicyProperties {

    /** 是否启用真实政策数据源；false 时回落 MockPolicyProvider */
    private boolean remoteEnabled = false;

    /** 所在省编码，广东省 */
    private String provinceCode = "440000";

    /** 所在市编码，深圳市（H5 端固定城市入参） */
    private String cityCode = "440300";

    /** 所在区编码，留空表示全市 */
    private String areaCode;

    /**
     * 启动/刷新时预取的页数（每页 50 条）。
     * DaaS 有约 10 次/分钟的限流，预取后由本地缓存提供分页与筛选，避免 C 端高频穿透。
     */
    private int prefetchPages = 8;

    /** 政策列表缓存时长（分钟） */
    private int listCacheMinutes = 30;

    /** 政策详情缓存时长（小时），正文相对稳定可长缓存 */
    private int detailCacheHours = 24;

    /** 单次远程调用失败后的最短重试间隔（秒），用于限流降级 */
    private int cooldownSeconds = 90;

}