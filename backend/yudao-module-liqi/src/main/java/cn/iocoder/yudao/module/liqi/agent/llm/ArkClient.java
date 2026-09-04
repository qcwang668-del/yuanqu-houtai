package cn.iocoder.yudao.module.liqi.agent.llm;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * 火山方舟对话客户端（OpenAI 兼容，SSE 流式）。
 *
 * <p>POST {arkUrl}，Authorization: Bearer {arkKey}，body {model, messages, tools?, stream:true}。
 * 流式解析 choices[0].delta 的 content（增量文本，回调下发）与 tool_calls（按 index 累积）。</p>
 */
@Slf4j
@Component
public class ArkClient {

    private static final int TIMEOUT = 120000;

    /**
     * 发起一轮流式对话。
     *
     * @param onDelta 每收到一段可见文本增量时回调（用于 SSE 下发前端）
     * @return 本轮结果（累积文本 + 工具调用 + finish_reason）
     */
    public ChatResult chat(String url, String apiKey, String model,
                           JSONArray messages, JSONArray tools, Consumer<String> onDelta) {
        JSONObject body = new JSONObject()
                .set("model", model)
                .set("messages", messages)
                .set("stream", true);
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", tools);
            body.set("tool_choice", "auto");
        }

        ChatResult result = new ChatResult();
        StringBuilder contentBuf = new StringBuilder();
        Map<Integer, ToolCall> toolMap = new TreeMap<>();

        HttpResponse resp = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .timeout(TIMEOUT)
                .body(body.toString())
                .executeAsync();
        if (!resp.isOk()) {
            String errBody = resp.body();
            resp.close();
            throw new RuntimeException("火山方舟返回 HTTP " + resp.getStatus() + "：" + StrUtil.sub(errBody, 0, 300));
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resp.bodyStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if ("[DONE]".equals(payload)) {
                    break;
                }
                if (!JSONUtil.isTypeJSON(payload)) {
                    continue;
                }
                JSONObject chunk = JSONUtil.parseObj(payload);
                JSONArray choices = chunk.getJSONArray("choices");
                if (choices == null || choices.isEmpty()) {
                    continue;
                }
                JSONObject choice = choices.getJSONObject(0);
                String finish = choice.getStr("finish_reason");
                if (StrUtil.isNotBlank(finish)) {
                    result.setFinishReason(finish);
                }
                JSONObject delta = choice.getJSONObject("delta");
                if (delta == null) {
                    continue;
                }
                String c = delta.getStr("content");
                if (StrUtil.isNotEmpty(c)) {
                    contentBuf.append(c);
                    if (onDelta != null) {
                        onDelta.accept(c);
                    }
                }
                JSONArray tcs = delta.getJSONArray("tool_calls");
                if (tcs != null) {
                    for (int i = 0; i < tcs.size(); i++) {
                        JSONObject tc = tcs.getJSONObject(i);
                        int idx = tc.getInt("index", i);
                        ToolCall builder = toolMap.computeIfAbsent(idx, k -> new ToolCall());
                        String id = tc.getStr("id");
                        if (StrUtil.isNotBlank(id)) {
                            builder.setId(id);
                        }
                        JSONObject fn = tc.getJSONObject("function");
                        if (fn != null) {
                            String name = fn.getStr("name");
                            if (StrUtil.isNotBlank(name)) {
                                builder.setName(name);
                            }
                            String argFrag = fn.getStr("arguments");
                            if (argFrag != null) {
                                builder.getArguments().append(argFrag);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("读取火山方舟流失败：" + e.getMessage(), e);
        } finally {
            resp.close();
        }

        result.setContent(contentBuf.toString());
        result.setToolCalls(new ArrayList<>(toolMap.values()));
        return result;
    }

    @Data
    public static class ChatResult {
        private String content = "";
        private List<ToolCall> toolCalls = new ArrayList<>();
        private String finishReason;

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    @Data
    public static class ToolCall {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();
    }

}
