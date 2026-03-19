package com.quiz.vo.app;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FavoriteVO {
    private Long id;
    private Long questionId;
    private String content;
    private Integer type;
    private String bankName;
    private String answer;
    private String analysis;
    private List<OptionVO> options;
    private LocalDateTime createTime;

    @Data
    public static class OptionVO {
        private String label;
        private String content;
    }
}
