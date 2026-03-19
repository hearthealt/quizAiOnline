package com.quiz.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 题目类型枚举
 */
@Getter
@AllArgsConstructor
public enum QuestionType {

    SINGLE(1, "单选题"),
    MULTIPLE(2, "多选题"),
    JUDGE(3, "判断题"),
    FILL(4, "填空题");

    private final int code;
    private final String desc;

    public static QuestionType of(int code) {
        for (QuestionType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的题目类型: " + code);
    }
}
