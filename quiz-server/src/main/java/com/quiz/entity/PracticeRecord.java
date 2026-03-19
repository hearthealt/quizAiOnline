package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("practice_record")
public class PracticeRecord implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private Long bankId;

    private String mode;

    private Integer totalCount;

    private Integer answerCount;

    private Integer correctCount;

    private Integer status;

    private Integer lastIndex;

    @JsonIgnore
    private String questionIds;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
