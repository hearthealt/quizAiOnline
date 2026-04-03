package com.quiz.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "开始练习请求")
public class StartPracticeDTO {

    @NotNull(message = "题库ID不能为空")
    @Min(value = 1, message = "题库ID无效")
    @Schema(description = "题库ID", required = true, example = "1")
    private Long bankId;

    @Pattern(regexp = "^(ORDER|RANDOM|WRONG)?$", message = "练习模式无效")
    @Schema(description = "练习模式：ORDER-顺序/RANDOM-随机/WRONG-错题", defaultValue = "ORDER")
    private String mode;

    @Schema(description = "是否重新练习（true=新建记录）", defaultValue = "false")
    private Boolean restart;
}
