package cn.iocoder.yudao.module.liqi.agent.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 创建会话 Request VO")
@Data
public class ConversationCreateReqVO {

    @Schema(description = "智能体编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "智能体编号不能为空")
    private Long agentId;

}
