package com.quiz.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiBatchJobVO {

    private Long id;

    private Long bankId;

    private String mode;

    private Integer overwrite;

    private Integer concurrency;

    private Integer totalCount;

    private Integer submittedCount;

    private Integer skippedCount;

    private Integer successCount;

    private Integer failCount;

    private Integer pendingCount;

    private Integer runningCount;

    private Integer status;

    private String statusText;

    private Integer progressPercent;

    private String errorMsg;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<AiBatchJobItemVO> recentFailedItems;
}
