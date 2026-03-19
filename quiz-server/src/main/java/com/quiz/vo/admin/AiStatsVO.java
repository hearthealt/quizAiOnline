package com.quiz.vo.admin;

import lombok.Data;

@Data
public class AiStatsVO {
    private Long totalCalls;
    private Long successCalls;
    private Long failCalls;
    private Long todayCalls;
    private Long totalTokens;
}
