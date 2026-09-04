package cn.iocoder.yudao.module.liqi.agent.mcp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP（Model Context Protocol）Streamable HTTP 客户端会话（JDK8，纯 hutool HTTP/1.1）。
 *
 * <p>规避坑：java.net.http 默认走 HTTP/2，对明文 http 会发 h2c 升级，FastMCP 返回 400；
 * hutool HttpRequest 天然 HTTP/1.1，可正常握手。</p>
 *
 * <p>完整握手：POST initialize（protocolVersion 2025-03-26）→ 捕获响应头 Mcp-Session-Id
 * → POST notifications/initialized → 之后可 tools/list、tools/call。</p>
 *
 * <p>响应体可能是纯 JSON，也可能是 SSE（text/event-stream，形如 "data: {json}\n\n"），两种都解析。</p>
 */
@Slf4j
public class McpSession {

    private static final String PROTOCOL_VERSION = "2025-03-26";
    private static final int TIMEOUT = 40000;

    private final String url;
    private final String token;
    private final AtomicInteger idSeq = new AtomicInteger(0);
    private String sessionId;
    private boolean connected = false;

    public McpSession(String url, String token) {
        this.url = url;
        this.token = token;
    }

    /** 完成 initialize + notifications/initialized 握手。失败抛异常，由上层优雅降级。 */
    public void connect() {
        JSONObject initParams = new JSONObject()
                .set("protocolVersion", PROTOCOL_VERSION)
                .set("capabilities", new JSONObject())
                .set("clientInfo", new JSONObject().set("name", "liqi-agent").set("version", "1.0"));
        JSONObject rpc = buildRpc("initialize", initParams);
        HttpResponse resp = post(rpc.toString());
        String sid = resp.header("Mcp-Session-Id");
        if (StrUtil.isBlank(sid)) {
            sid = resp.header("mcp-session-id");
        }
        if (StrUtil.isNotBlank(sid)) {
            this.sessionId = sid;
        }
        JSONObject result = extractResult(resp.body());
        if (result == null) {
            throw new RuntimeException("MCP initialize 无 result，body=" + StrUtil.sub(resp.body(), 0, 200));
        }
        // 通知 initialized（notification，无 id，无需解析响应）
        JSONObject notify = new JSONObject()
                .set("jsonrpc", "2.0")
                .set("method", "notifications/initialized");
        try {
            post(notify.toString()).close();
        } catch (Exception e) {
            log.warn("[MCP] notifications/initialized 忽略异常：{}", e.getMessage());
        }
        this.connected = true;
        log.info("[MCP] 握手完成 url={} sessionId={}", url, sessionId);
    }

    /** 列出工具定义（name/description/inputSchema）。 */
    public List<JSONObject> listTools() {
        ensureConnected();
        JSONObject rpc = buildRpc("tools/list", new JSONObject());
        JSONObject result = extractResult(post(rpc.toString()).body());
        List<JSONObject> tools = new ArrayList<>();
        if (result == null) {
            return tools;
        }
        JSONArray arr = result.getJSONArray("tools");
        if (arr == null) {
            return tools;
        }
        for (int i = 0; i < arr.size(); i++) {
            tools.add(arr.getJSONObject(i));
        }
        return tools;
    }

    /** 调用工具，返回结果文本（content[].text 拼接）。 */
    public String callTool(String name, Map<String, Object> args) {
        ensureConnected();
        JSONObject params = new JSONObject()
                .set("name", name)
                .set("arguments", args == null ? new JSONObject() : new JSONObject(args));
        JSONObject rpc = buildRpc("tools/call", params);
        JSONObject result = extractResult(post(rpc.toString()).body());
        if (result == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        JSONArray content = result.getJSONArray("content");
        if (content != null) {
            for (int i = 0; i < content.size(); i++) {
                JSONObject c = content.getJSONObject(i);
                String text = c.getStr("text");
                if (StrUtil.isNotBlank(text)) {
                    sb.append(text);
                }
            }
        }
        if (sb.length() == 0) {
            // 没有 text 内容时，回退整个 result 文本，便于留痕
            sb.append(result.toString());
        }
        return sb.toString();
    }

    // ================= 内部 =================

    private void ensureConnected() {
        if (!connected) {
            connect();
        }
    }

    private JSONObject buildRpc(String method, JSONObject params) {
        return new JSONObject()
                .set("jsonrpc", "2.0")
                .set("id", idSeq.incrementAndGet())
                .set("method", method)
                .set("params", params);
    }

    private HttpResponse post(String body) {
        HttpRequest req = HttpRequest.post(url)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .timeout(TIMEOUT)
                .body(body);
        if (StrUtil.isNotBlank(sessionId)) {
            req.header("Mcp-Session-Id", sessionId);
        }
        return req.execute();
    }

    /**
     * 从响应体解析出 JSON-RPC 的 result。兼容纯 JSON 与 SSE（data: 行）两种格式。
     */
    private JSONObject extractResult(String body) {
        if (StrUtil.isBlank(body)) {
            return null;
        }
        String json = body.trim();
        if (!JSONUtil.isTypeJSON(json)) {
            // SSE：收集所有 data: 行的负载拼接
            StringBuilder data = new StringBuilder();
            for (String line : body.split("\n")) {
                String l = line.trim();
                if (l.startsWith("data:")) {
                    data.append(l.substring(5).trim());
                }
            }
            json = data.toString();
        }
        if (!JSONUtil.isTypeJSON(json)) {
            log.warn("[MCP] 响应非 JSON：{}", StrUtil.sub(body, 0, 200));
            return null;
        }
        JSONObject obj = JSONUtil.parseObj(json);
        if (obj.containsKey("error")) {
            log.warn("[MCP] JSON-RPC error：{}", obj.getStr("error"));
            return null;
        }
        return obj.getJSONObject("result");
    }

}
