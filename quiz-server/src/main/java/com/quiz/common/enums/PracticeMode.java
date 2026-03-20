package com.quiz.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 练习模式枚举
 */
@Getter
@AllArgsConstructor
public enum PracticeMode {

    ORDER("ORDER", "顺序练习"),
    RANDOM("RANDOM", "随机练习"),
    WRONG("WRONG", "错题重练");

    private final String code;
    private final String desc;

    public static PracticeMode of(String code) {
        if (code == null) {
            return ORDER;
        }
        for (PracticeMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        return ORDER;
    }

    public boolean isWrong() {
        return this == WRONG;
    }

    public boolean isRandom() {
        return this == RANDOM;
    }
}
