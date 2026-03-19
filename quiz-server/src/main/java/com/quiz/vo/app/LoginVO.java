package com.quiz.vo.app;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private UserInfoVO userInfo;
}
