package com.quiz.vo.app;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer isVip;
    private LocalDateTime vipExpireTime;
}
