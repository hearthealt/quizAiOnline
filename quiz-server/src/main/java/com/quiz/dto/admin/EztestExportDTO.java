package com.quiz.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class EztestExportDTO {

    private String permit;

    private List<String> sessionIds;

    private Boolean exportXlsx;

    private Boolean exportPdfWithAnswers;

    private Boolean exportPdfWithoutAnswers;

    private Long importBankId;

    private Long importCategoryId;
}
