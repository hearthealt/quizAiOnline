package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("eztest_job")
public class EztestJob implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String permitMasked;

    private String sessionIds;

    private String sessionNames;

    private Long importBankId;

    private Long importCategoryId;

    private String importPayload;

    private Integer exportXlsx;

    private Integer exportPdfWithAnswers;

    private Integer exportPdfWithoutAnswers;

    private Integer sessionCount;

    private Integer completedCount;

    private Integer progressPercent;

    private Integer rawCount;

    private Integer exportedCount;

    private Integer duplicateCount;

    private Integer importCreateCount;

    private Integer importUpdateCount;

    private Integer importFailCount;

    /**
     * 状态: 0-排队中 1-执行中 2-已完成 3-完成但有失败 4-执行异常
     */
    private Integer status;

    private String progressText;

    private String errorMsg;

    private String logs;

    private Long operatorId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
