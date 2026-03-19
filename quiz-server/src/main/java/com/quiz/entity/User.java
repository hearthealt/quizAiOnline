package com.quiz.entity;

import com.mybatisflex.annotation.Table;
import com.quiz.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("user")
public class User extends BaseEntity {

    private String openid;

    private String unionId;

    private String phone;

    private String password;

    private String nickname;

    private String avatar;

    private Integer status;

    private Integer isVip;

    private LocalDateTime vipExpireTime;

    private String settings;

    private LocalDateTime lastLoginTime;
}
