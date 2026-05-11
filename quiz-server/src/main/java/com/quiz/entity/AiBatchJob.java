package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("ai_batch_job")
public class AiBatchJob implements Serializable {

    @Id(keyType = KeyType.Auto)
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

    /**
     * 状态: 0-排队中 1-执行中 2-已完成 3-完成但有失败 4-执行异常 5-已暂停 6-已取消
     */
    private Integer status;

    private String errorMsg;

    private Long operatorId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
