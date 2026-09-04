package cn.iocoder.yudao.module.liqi.agent.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 智能体广场配置（凭据仅由 GPU 启动进程的环境变量注入，禁止硬编码进仓库）。
 */
@Data
@Component
public class LiqiAgentProperties {

    /** 火山方舟 API Key（env: LIQI_ARK_KEY） */
    @Value("${liqi.agent.ark-key:}")
    private String arkKey;

    /** 火山方舟 OpenAI 兼容对话地址 */
    @Value("${liqi.agent.ark-url:https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions}")
    private String arkUrl;

    /** 工商 MCP 访问 token（env: LIQI_MCP_TOKEN） */
    @Value("${liqi.agent.mcp-token:}")
    private String mcpToken;

}
