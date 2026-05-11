package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.AiBatchGenerateDTO;
import com.quiz.dto.admin.AiConfigDTO;
import com.quiz.dto.admin.AiLogQueryDTO;
import com.quiz.entity.AiConfig;
import com.quiz.vo.admin.AiBatchJobVO;
import com.quiz.vo.admin.AiCallLogVO;
import com.quiz.vo.admin.AiStatsVO;

import java.util.List;
import java.util.Map;

public interface AiAnalysisService {

    AiConfig getConfig();

    void updateConfig(AiConfigDTO dto);

    Map<String, Object> testConnection(AiConfigDTO dto);

    List<Map<String, Object>> listModels(AiConfigDTO dto);

    String generate(Long questionId, String mode, Long operatorId);

    /**
     * 异步生成AI解析（立即返回，后台执行）
     */
    void generateAsync(Long questionId, String mode, Long operatorId);

    Map<String, Object> batchGenerate(AiBatchGenerateDTO dto, Long operatorId);

    AiBatchJobVO getBatchJob(Long jobId);

    PageResult<AiBatchJobVO> getBatchJobList(Integer pageNum, Integer pageSize, Integer status, String mode);

    void pauseBatchJob(Long jobId);

    void resumeBatchJob(Long jobId);

    void retryFailedBatchJob(Long jobId);

    void cancelBatchJob(Long jobId);

    void deleteBatchJob(Long jobId);

    PageResult<AiCallLogVO> getLogList(AiLogQueryDTO query);

    AiStatsVO getAiStats();
}
