package com.quiz.vo.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EztestJobVO {

    private Long id;

    private String permitMasked;

    private List<String> sessionIds;

    private List<String> sessionNames;

    private Long importBankId;

    private Long importCategoryId;

    private Integer importable;

    private Integer exportXlsx;

    private Integer exportPdfWithAnswers;

    private Integer exportPdfWithoutAnswers;

    private Integer sessionCount;

    private Integer completedCount;

    private Integer rawCount;

    private Integer exportedCount;

    private Integer duplicateCount;

    private Integer importCreateCount;

    private Integer importUpdateCount;

    private Integer importFailCount;

    private Integer status;

    private String statusText;

    private Integer progressPercent;

    private String progressText;

    private String errorMsg;

    private String logs;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<EztestJobFileVO> files;
}
