package com.quiz.vo.admin;

import lombok.Data;

import java.util.List;

@Data
public class DashboardVO {
    private Long totalUsers;
    private Long totalQuestions;
    private Long todayActive;
    private Long todayAnswers;
    private List<TrendItem> trends;

    @Data
    public static class TrendItem {
        private String date;
        private Long count;
    }
}
