package com.quiz.vo.app;

import lombok.Data;

import java.util.List;

@Data
public class HomeVO {
    private List<CategoryVO> categories;
    private List<BankSimpleVO> hotBanks;
    private QuestionVO dailyQuestion;
    private StudyStatsVO studyStats;

    @Data
    public static class CategoryVO {
        private Long id;
        private String name;
        private String icon;
        private Integer bankCount;
    }

    @Data
    public static class BankSimpleVO {
        private Long id;
        private String name;
        private String cover;
        private Integer questionCount;
        private Integer practiceCount;
    }
}
