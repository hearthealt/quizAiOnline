package com.quiz.dto.admin;

import lombok.Data;

@Data
public class AiLogQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     * 调用类型: ADMIN/USER
     */
    private String callType;

    /**
     * 状态: 0-失败 1-成功
     */
    private Integer status;

    /**
     * 调用模式: CHAT/GENERATE_ANALYSIS/GENERATE_ANSWER/GENERATE_BOTH
     */
    private String mode;
}
