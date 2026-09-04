package cn.iocoder.yudao.module.liqi.service.enterprise.qiyedata;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 企业数据平台（qiyedata）客户端 · 企业基本信息第三步接口。
 *
 * <p>请求头签名规则：appKey + timestamp（毫秒，有效期 5 分钟）+ nonce（每次新 UUID）
 * + sign = MD5(appKey + timestamp + nonce + appSecret)。appSecret 仅本地参与签名，不随请求发送。</p>
 *
 * <p><b>使用范围限制</b>：该接口凭据仅授权本项目（力企 SaaS）内部调用，
 * 不得对外暴露为代理接口供其他外部项目请求，详见 {@code LiqiEnterpriseController#syncBaseInfo}。</p>
 */
@Slf4j
@Component
public class QiyeDataClient {

    @Resource
    private QiyeDataProperties properties;

    /**
     * 查询企业基本信息。
     *
     * @param entityId 统一社会信用代码 / 平台唯一 id
     * @return 基本信息；未开通配置、查无结果或调用失败返回 null
     */
    public QiyeBaseInfoDTO baseInfo(String entityId) {
        if (StrUtil.isBlank(entityId)) {
            return null;
        }
        if (!properties.available()) {
            log.warn("[QiyeDataClient] 企业数据平台未配置（base-url/app-key/app-secret 缺失），跳过调用");
            return null;
        }
        String url = StrUtil.removeSuffix(properties.getBaseUrl(), "/") + properties.getBaseInfoPath()
                + "?entityId=" + URLUtil.encode(entityId);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = IdUtil.fastSimpleUUID();
        String text;
        try {
            HttpRequest request = HttpRequest.get(url)
                    .header("appKey", properties.getAppKey())
                    .header("timestamp", timestamp)
                    .header("nonce", nonce)
                    .header("sign", sign(timestamp, nonce))
                    .timeout(properties.getTimeout() == null ? 20000 : properties.getTimeout());
            if (StrUtil.isNotBlank(properties.getTenantId())) {
                request.header("tenant-id", properties.getTenantId());
            }
            text = request.execute().body();
        } catch (Exception e) {
            log.warn("[QiyeDataClient] 请求异常 entityId={}", entityId, e);
            return null;
        }
        if (!JSONUtil.isTypeJSONObject(text)) {
            log.warn("[QiyeDataClient] 返回非 JSON：{}", StrUtil.sub(text, 0, 200));
            return null;
        }
        JSONObject raw = JSONUtil.parseObj(text);
        // 平台成功码兼容：文档示例为 0，实际返回 200
        int code = raw.getInt("code", -1);
        if (code != 0 && code != 200) {
            log.warn("[QiyeDataClient] 接口返回失败 code={} msg={}", code, raw.getStr("msg"));
            return null;
        }
        JSONObject data = raw.getJSONObject("data");
        if (data == null || data.isEmpty()) {
            return null;
        }
        return data.toBean(QiyeBaseInfoDTO.class);
    }

    /** sign = MD5(appKey + timestamp + nonce + appSecret) */
    private String sign(String timestamp, String nonce) {
        return DigestUtil.md5Hex(properties.getAppKey() + timestamp + nonce + properties.getAppSecret());
    }

}
