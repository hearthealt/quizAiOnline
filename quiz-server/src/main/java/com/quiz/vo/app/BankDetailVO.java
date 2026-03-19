package com.quiz.vo.app;

import lombok.Data;

@Data
public class BankDetailVO {
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private String cover;
    private Integer questionCount;
    private Integer practiceCount;
    /**
     * 当前练习总题数（进行中时使用）
     */
    private Integer practiceTotalCount;
    private Integer examTime;
    private Integer passScore;
    /**
     * 已答题数
     */
    private Integer userProgress;
    private Double userCorrectRate;
}
