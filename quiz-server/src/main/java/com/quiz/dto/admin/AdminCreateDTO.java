package com.quiz.dto.admin;

import lombok.Data;

@Data
public class AdminCreateDTO {
    private String username;
    private String password;
    private String role;
}
