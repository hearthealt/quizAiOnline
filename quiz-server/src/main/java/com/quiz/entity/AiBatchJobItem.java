package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("ai_batch_job_item")
public class AiBatchJobItem implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long jobId;

    private Long questionId;

    /**
     * 状态: 0-待处理 1-处理中 2-成功 3-失败 4-已取消
     */
    private Integer status;

    private Integer retryCount;

    private String errorMsg;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
