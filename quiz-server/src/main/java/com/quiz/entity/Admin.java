package com.quiz.entity;

import com.mybatisflex.annotation.Table;
import com.quiz.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("admin")
public class Admin extends BaseEntity {

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String role;

    private Integer status;

    private LocalDateTime lastLoginTime;
}
