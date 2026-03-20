package com.quiz.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 题目难度等级枚举
 */
@Getter
@AllArgsConstructor
public enum Difficulty {

    EASY(1, "简单"),
    MEDIUM(2, "中等"),
    HARD(3, "困难");

    private final int code;
    private final String desc;

    public static Difficulty of(int code) {
        for (Difficulty d : values()) {
            if (d.code == code) {
                return d;
            }
        }
        return EASY;
    }

    public static Difficulty fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return EASY;
        }
        return switch (str.trim()) {
            case "简单" -> EASY;
            case "中等" -> MEDIUM;
            case "困难" -> HARD;
            default -> EASY;
        };
    }
}
