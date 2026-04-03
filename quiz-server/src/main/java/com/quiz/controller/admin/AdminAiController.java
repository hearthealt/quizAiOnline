package com.quiz.controller.admin;

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
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        aiAnalysisService.generateAsync(dto.getQuestionId(), dto.getMode(), adminId);
        return R.ok("已提交AI生成任务");
    }

    @Operation(summary = "批量生成AI解析")
    @PostMapping("/batch-generate")
    public R<?> batchGenerate(@RequestBody AiBatchGenerateDTO dto) {
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        return R.ok(aiAnalysisService.batchGenerate(dto, adminId));
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
