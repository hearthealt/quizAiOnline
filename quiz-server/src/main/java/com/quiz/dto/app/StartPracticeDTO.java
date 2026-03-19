package com.quiz.dto.app;

import lombok.Data;

@Data
public class StartPracticeDTO {
    private Long bankId;
    /**
     * 练习模式：ORDER-顺序/RANDOM-随机/WRONG-错题
     */
    private String mode;
    /**
     * 练习题数（不传则用系统配置，0=全部）
     */
    private Integer count;
    /**
     * 是否重新练习（true=新建记录）
     */
    private Boolean restart;
}
