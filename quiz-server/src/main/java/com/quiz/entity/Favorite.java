package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("favorite")
public class Favorite implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private Long questionId;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}
