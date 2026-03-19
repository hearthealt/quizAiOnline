package com.quiz.vo.app;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecordVO {
    private Long id;
    private String bankName;
    private String mode;
    private Integer totalCount;
    private Integer correctCount;
    private Double correctRate;
    /** 考试得分 */
    private Integer score;
    private LocalDateTime createTime;
    /**
     * practice/exam
     */
    private String type;
}
