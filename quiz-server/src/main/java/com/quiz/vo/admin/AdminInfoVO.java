package com.quiz.vo.admin;

import lombok.Data;

@Data
public class AdminInfoVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private String token;
}
