package com.quiz.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EztestJobFileVO {

    private Long id;

    private Long jobId;

    private String fileType;

    private String fileTypeText;

    private String fileName;

    private Long fileSize;

    private LocalDateTime createTime;
}
