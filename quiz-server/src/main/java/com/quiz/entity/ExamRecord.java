package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("exam_record")
public class ExamRecord implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private Long bankId;

    private Integer totalCount;

    private Integer correctCount;

    private Integer score;

    private Integer duration;

    private Integer passScore;

    private Integer isPass;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}
