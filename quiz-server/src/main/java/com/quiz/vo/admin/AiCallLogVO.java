package com.quiz.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiCallLogVO {

    private Long id;

    private Long questionId;

    /**
     * 调用类型: ADMIN-管理端 USER-用户端
     */
    private String callType;

    private String mode;

    private String prompt;

    private String result;

    private Integer tokensUsed;

    private Integer costMs;

    private Integer status;

    private String errorMsg;

    private Long operatorId;

    private Long userId;

    /**
     * 操作人昵称（管理员昵称或用户昵称）
     */
    private String operatorName;

    private LocalDateTime createTime;
}
