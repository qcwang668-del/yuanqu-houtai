package cn.iocoder.yudao.module.liqi.agent.mcp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP 客户端工厂 + 工具定义转换（MCP tool → OpenAI function）。
 */
@Slf4j
@Component
public class McpClient {

    /** 打开一个 MCP 会话（尚未握手，首次调用时自动 connect）。 */
    public McpSession openSession(String url, String token) {
        return new McpSession(url, token);
    }

    /**
     * 把 MCP 工具定义列表转换为 OpenAI function-calling 的 tools 数组。
     * MCP: {name, description, inputSchema} → OpenAI: {type:function, function:{name,description,parameters}}
     */
    public JSONArray toOpenAiTools(List<JSONObject> mcpTools) {
        JSONArray arr = new JSONArray();
        if (mcpTools == null) {
            return arr;
        }
        for (JSONObject t : mcpTools) {
            JSONObject fn = new JSONObject()
                    .set("name", t.getStr("name"))
                    .set("description", t.getStr("description", ""));
            JSONObject schema = t.getJSONObject("inputSchema");
            if (schema == null) {
                schema = new JSONObject().set("type", "object").set("properties", new JSONObject());
            }
            fn.set("parameters", schema);
            arr.add(new JSONObject().set("type", "function").set("function", fn));
        }
        return arr;
    }

}
