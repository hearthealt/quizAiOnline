package com.quiz.vo.app;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VipInfoVO {
    private Boolean isVip;
    private LocalDateTime expireTime;
    private List<PlanVO> plans;

    @Data
    public static class PlanVO {
        private Long id;
        private String name;
        private Integer duration;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private String description;
    }
}
