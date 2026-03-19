package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("wrong_question")
public class WrongQuestion implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private Long questionId;

    private Long bankId;

    private Integer wrongCount;

    private String lastWrongAnswer;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
