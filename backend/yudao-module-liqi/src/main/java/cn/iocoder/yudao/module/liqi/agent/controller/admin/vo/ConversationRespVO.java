package cn.iocoder.yudao.module.liqi.agent.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 智能体会话 Response VO")
@Data
public class ConversationRespVO {

    @Schema(description = "编号")
    private Long id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
