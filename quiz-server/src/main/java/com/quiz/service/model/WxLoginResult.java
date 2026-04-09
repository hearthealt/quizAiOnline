package com.quiz.service.model;

import com.quiz.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WxLoginResult {
    private User user;
    private boolean newUser;
}
