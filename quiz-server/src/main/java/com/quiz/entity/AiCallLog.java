package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("ai_call_log")
public class AiCallLog implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long questionId;

    /**
     * 调用类型: ADMIN-管理端 USER-用户端
     */
    private String callType;

    private String mode;

    /**
     * 实际调用链路: responses / chat / chat-minimal / chat-stream-fallback
     */
    private String route;

    private String prompt;

    private String result;

    private Integer tokensUsed;

    private Integer costMs;

    private Integer status;

    private String errorMsg;

    /**
     * 操作管理员ID（管理端调用时记录）
     */
    private Long operatorId;

    /**
     * 用户ID（用户端调用时记录）
     */
    private Long userId;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}
