package com.quiz.vo.app;

import lombok.Data;

@Data
public class ExamOngoingVO {

    private boolean exists;

    private boolean expired;

    private Long examId;

    /**
     * 剩余时间（秒）
     */
    private Integer leftSeconds;
}
