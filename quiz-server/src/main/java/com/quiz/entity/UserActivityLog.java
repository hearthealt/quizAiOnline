package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("user_activity_log")
public class UserActivityLog implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private LocalDate activityDate;

    private String actionType;

    private String bizType;

    private Long bizId;

    private Long bankId;

    private Long questionId;

    private Long recordId;

    private String userAnswer;

    private Integer isCorrect;

    private Integer costSeconds;

    private String extJson;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}
