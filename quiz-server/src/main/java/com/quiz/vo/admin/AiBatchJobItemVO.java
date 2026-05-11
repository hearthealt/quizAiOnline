package com.quiz.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiBatchJobItemVO {

    private Long id;

    private Long jobId;

    private Long questionId;

    private Integer status;

    private String statusText;

    private Integer retryCount;

    private String errorMsg;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
