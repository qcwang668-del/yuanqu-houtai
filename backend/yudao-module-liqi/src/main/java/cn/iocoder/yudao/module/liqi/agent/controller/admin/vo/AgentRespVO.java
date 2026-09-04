package cn.iocoder.yudao.module.liqi.agent.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 智能体 Response VO")
@Data
public class AgentRespVO {

    @Schema(description = "编号")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "是否启用 MCP 工具")
    private Boolean mcpEnabled;

}
