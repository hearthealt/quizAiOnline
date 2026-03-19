package com.quiz.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 考试状态枚举
 */
@Getter
@AllArgsConstructor
public enum ExamStatus {

    IN_PROGRESS(0, "进行中"),
    FINISHED(1, "已完成");

    private final int code;
    private final String desc;

    public static ExamStatus of(int code) {
        for (ExamStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的考试状态: " + code);
    }
}
