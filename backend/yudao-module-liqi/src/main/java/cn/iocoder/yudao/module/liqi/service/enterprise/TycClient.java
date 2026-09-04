package cn.iocoder.yudao.module.liqi.service.enterprise;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 天眼查客户端（经蓝凌 openapi 代理调用，凭据内置，仅只读）。
 * 仅开放：工商基本信息 baseinfo + 股东 holder（其余接口未开放）。
 */
@Slf4j
@Component
public class TycClient {

    @Value("${liqi.tyc.proxy-url:}")
    private String proxyUrl;
    @Value("${liqi.tyc.proxy-auth:}")
    private String proxyAuth;
    @Value("${liqi.tyc.api-base:http://open.api.tianyancha.com/services}")
    private String apiBase;
    @Value("${liqi.tyc.token:}")
    private String token;

    /** 工商基本信息；查无结果或异常返回 null。keyword 必须是工商登记全称或统一社会信用代码。 */
    public JSONObject baseInfo(String keyword) {
        JSONObject result = call("open/ic/baseinfo/normal?keyword=" + URLUtil.encode(keyword));
        return result;
    }

    /** 股东列表（name + percent）。查无返回空 List。 */
    public List<String[]> holders(String keyword) {
        List<String[]> list = new ArrayList<>();
        JSONObject result = call("open/ic/holder/2.0?keyword=" + URLUtil.encode(keyword));
        if (result == null) {
            return list;
        }
        JSONArray items = result.getJSONArray("items");
        if (items == null) {
            return list;
        }
        for (int i = 0; i < items.size(); i++) {
            JSONObject it = items.getJSONObject(i);
            String name = it.getStr("name", "");
            String percent = "";
            JSONArray cap = it.getJSONArray("capital");
            if (cap != null && !cap.isEmpty()) {
                percent = cap.getJSONObject(0).getStr("percent", "");
            }
            if (StrUtil.isNotBlank(name)) {
                list.add(new String[]{name, percent});
            }
        }
        return list;
    }

    /** 调用代理并解包，返回 data.result（查无结果 error_code=300000 → null）。 */
    private JSONObject call(String tycPath) {
        if (StrUtil.isBlank(proxyUrl) || StrUtil.isBlank(token)) {
            log.warn("[TycClient] 天眼查凭据未配置（liqi.tyc.proxy-url / liqi.tyc.token），跳过调用");
            return null;
        }
        String fdUrl = apiBase + "/" + tycPath;
        JSONObject body = new JSONObject().set("fdUrl", fdUrl).set("fdToken", token);
        String text;
        try {
            text = HttpRequest.post(proxyUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", proxyAuth)
                    .body(body.toString())
                    .timeout(40000)
                    .execute().body();
        } catch (Exception e) {
            log.warn("[TycClient] 代理请求异常 path={}", tycPath, e);
            return null;
        }
        if (!JSONUtil.isTypeJSON(text)) {
            log.warn("[TycClient] 代理返回非 JSON：{}", StrUtil.sub(text, 0, 200));
            return null;
        }
        JSONObject raw = JSONUtil.parseObj(text);
        if (Boolean.FALSE.equals(raw.getBool("success"))) {
            log.warn("[TycClient] 蓝凌代理执行失败：{}", raw.getStr("msg"));
            return null;
        }
        JSONObject data = raw.getJSONObject("data");
        if (data == null) {
            return null;
        }
        if (data.containsKey("error_code")) {
            int code = data.getInt("error_code", -1);
            if (code == 300000) {
                return null; // 查无结果
            }
            if (code != 0 && code != 200) {
                log.warn("[TycClient] 天眼查错误[{}]：{}", code, data.getStr("reason"));
                return null;
            }
        }
        return data.getJSONObject("result");
    }

}
