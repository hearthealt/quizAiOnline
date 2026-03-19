package com.quiz.dto.admin;

import lombok.Data;

@Data
public class AdminUpdateDTO {
    private String username;
    private String nickname;
    private String avatar;
    private String role;
}
