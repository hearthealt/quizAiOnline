package com.quiz.vo.admin;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsVO {
    private List<TrendItem> userGrowth;
    private List<TrendItem> answerTrend;
    private List<RankItem> bankHotRank;
    private List<RankItem> wrongRank;

    @Data
    public static class TrendItem {
        private String date;
        private Long count;
    }

    @Data
    public static class RankItem {
        private String name;
        private Long count;
    }
}
