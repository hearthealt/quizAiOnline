package com.quiz.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "开始考试请求")
public class StartExamDTO {

    @NotNull(message = "题库ID不能为空")
    @Min(value = 1, message = "题库ID无效")
    @Schema(description = "题库ID", required = true, example = "1")
    private Long bankId;

    @Schema(description = "是否重新开始考试（true=结束当前未完成考试并重新开始）", defaultValue = "false")
    private Boolean restart;
}
