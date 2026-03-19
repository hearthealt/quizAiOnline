package com.quiz.vo.app;

import lombok.Data;

@Data
public class StudyStatsVO {
    private Integer totalDays;
    private Integer totalAnswered;
    private Double correctRate;
    private Integer todayAnswered;
}
