package com.quiz.vo.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VipStatsVO {
    private Long totalVipUsers;
    private BigDecimal totalRevenue;
    private BigDecimal monthRevenue;
    private Long activeVipCount;
}
