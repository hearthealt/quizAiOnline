package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("eztest_job_file")
public class EztestJobFile implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long jobId;

    /**
     * XLSX / PDF_WITH_ANSWERS / PDF_WITHOUT_ANSWERS
     */
    private String fileType;

    private String fileName;

    private String filePath;

    private Long fileSize;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}
