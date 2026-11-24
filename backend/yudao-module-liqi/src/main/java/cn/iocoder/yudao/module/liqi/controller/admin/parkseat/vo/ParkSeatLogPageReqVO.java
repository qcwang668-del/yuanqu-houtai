package cn.iocoder.yudao.module.liqi.controller.admin.parkseat.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 园区席位指派记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ParkSeatLogPageReqVO extends PageParam {

    private Long parkId;
    private String keyword;
}
