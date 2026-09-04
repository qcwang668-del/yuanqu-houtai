package cn.iocoder.yudao.module.liqi.agent.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 智能体消息 Response VO")
@Data
public class MessageRespVO {

    @Schema(description = "角色：user/assistant/tool")
    private String role;
    @Schema(description = "内容")
    private String content;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
