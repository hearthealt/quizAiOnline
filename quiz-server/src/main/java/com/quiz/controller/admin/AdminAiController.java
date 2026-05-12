package com.quiz.controller.admin;

import com.quiz.common.constant.CommonConstant;
import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.dto.admin.AiBatchGenerateDTO;
import com.quiz.dto.admin.AiConfigDTO;
import com.quiz.dto.admin.AiGenerateDTO;
import com.quiz.dto.admin.AiLogQueryDTO;
import com.quiz.service.AiAnalysisService;
import com.quiz.vo.admin.AiStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI管理")
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiAnalysisService aiAnalysisService;

    @Operation(summary = "获取AI配置")
    @GetMapping("/config")
    public R<?> getConfig() {
        return R.ok(aiAnalysisService.getConfig());
    }

    @Operation(summary = "更新AI配置")
    @PutMapping("/config")
    public R<Void> updateConfig(@RequestBody AiConfigDTO dto) {
        aiAnalysisService.updateConfig(dto);
        return R.ok();
    }

    @Operation(summary = "测试AI连接")
    @PostMapping("/test")
    public R<?> testConnection(@RequestBody(required = false) AiConfigDTO dto) {
        return R.ok(aiAnalysisService.testConnection(dto));
    }

    @Operation(summary = "获取模型列表")
    @PostMapping("/models")
    public R<?> models(@RequestBody AiConfigDTO dto) {
        return R.ok(aiAnalysisService.listModels(dto));
    }

    @Operation(summary = "生成AI解析")
    @PostMapping("/generate")
    public R<?> generate(@RequestBody AiGenerateDTO dto) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        aiAnalysisService.generateAsync(dto.getQuestionId(), dto.getMode(), adminId);
        return R.ok("已提交AI生成任务");
    }

    @Operation(summary = "批量生成AI解析")
    @PostMapping("/batch-generate")
    public R<?> batchGenerate(@RequestBody AiBatchGenerateDTO dto) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        return R.ok(aiAnalysisService.batchGenerate(dto, adminId));
    }

    @Operation(summary = "AI批量任务详情")
    @GetMapping("/batch-job/{id}")
    public R<?> batchJob(@PathVariable Long id) {
        return R.ok(aiAnalysisService.getBatchJob(id));
    }

    @Operation(summary = "AI批量任务列表")
    @GetMapping("/batch-job/list")
    public R<?> batchJobList(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) Integer status,
                             @RequestParam(required = false) String mode) {
        return R.ok(aiAnalysisService.getBatchJobList(pageNum, pageSize, status, mode));
    }

    @Operation(summary = "暂停AI批量任务")
    @PostMapping("/batch-job/{id}/pause")
    public R<Void> pauseBatchJob(@PathVariable Long id) {
        aiAnalysisService.pauseBatchJob(id);
        return R.ok();
    }

    @Operation(summary = "继续AI批量任务")
    @PostMapping("/batch-job/{id}/resume")
    public R<Void> resumeBatchJob(@PathVariable Long id) {
        aiAnalysisService.resumeBatchJob(id);
        return R.ok();
    }

    @Operation(summary = "重试AI批量任务失败题目")
    @PostMapping("/batch-job/{id}/retry-failed")
    public R<Void> retryFailedBatchJob(@PathVariable Long id) {
        aiAnalysisService.retryFailedBatchJob(id);
        return R.ok();
    }

    @Operation(summary = "取消AI批量任务")
    @PostMapping("/batch-job/{id}/cancel")
    public R<Void> cancelBatchJob(@PathVariable Long id) {
        aiAnalysisService.cancelBatchJob(id);
        return R.ok();
    }

    @Operation(summary = "删除AI批量任务")
    @DeleteMapping("/batch-job/{id}")
    public R<Void> deleteBatchJob(@PathVariable Long id) {
        aiAnalysisService.deleteBatchJob(id);
        return R.ok();
    }

    @Operation(summary = "AI日志列表")
    @GetMapping("/log/list")
    public R<?> logList(AiLogQueryDTO query) {
        return R.ok(aiAnalysisService.getLogList(query));
    }

    @Operation(summary = "AI统计")
    @GetMapping("/stats")
    public R<AiStatsVO> stats() {
        return R.ok(aiAnalysisService.getAiStats());
    }
}
