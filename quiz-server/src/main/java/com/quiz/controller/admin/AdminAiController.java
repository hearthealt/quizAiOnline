package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.dto.admin.AiBatchGenerateDTO;
import com.quiz.dto.admin.AiConfigDTO;
import com.quiz.dto.admin.AiGenerateDTO;
import com.quiz.dto.admin.AiLogQueryDTO;
import com.quiz.dto.admin.IFlowApiKeyResult;
import com.quiz.entity.AiConfig;
import com.quiz.service.AiAnalysisService;
import com.quiz.service.AiConvertService;
import com.quiz.service.IFlowApiKeyService;
import com.quiz.util.FileParseUtil;
import com.quiz.vo.admin.AiStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI管理")
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiAnalysisService aiAnalysisService;
    private final IFlowApiKeyService iFlowApiKeyService;
    private final AiConvertService aiConvertService;

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
    public R<?> testConnection() {
        return R.ok(aiAnalysisService.testConnection());
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

    @Operation(summary = "获取iFlow可用模型列表")
    @GetMapping("/iflow/models")
    public R<?> iflowModels() {
        AiConfig config = aiAnalysisService.getConfig();
        if (config == null || config.getBxAuth() == null || config.getBxAuth().isEmpty()) {
            return R.ok(java.util.Collections.emptyList());
        }
        return R.ok(iFlowApiKeyService.getModelList(config));
    }

    @Operation(summary = "获取iFlow API Key状态")
    @GetMapping("/iflow/status")
    public R<?> iflowStatus() {
        AiConfig config = aiAnalysisService.getConfig();
        if (config == null || config.getBxAuth() == null || config.getBxAuth().isEmpty()) {
            return R.ok(null);
        }
        IFlowApiKeyResult result = iFlowApiKeyService.getApiKeyInfo(config);
        return R.ok(result);
    }

    @Operation(summary = "手动刷新iFlow API Key")
    @PostMapping("/iflow/refresh")
    public R<?> iflowRefresh() {
        AiConfig config = aiAnalysisService.getConfig();
        if (config == null) {
            return R.fail("AI配置不存在");
        }
        if (config.getBxAuth() == null || config.getBxAuth().isEmpty()) {
            return R.fail("请先配置BXAuth");
        }
        IFlowApiKeyResult result = iFlowApiKeyService.refreshApiKey(config);
        if (result.getSuccess()) {
            return R.ok(result);
        } else {
            return R.fail(result.getError());
        }
    }

    @Operation(summary = "解析文件内容")
    @PostMapping("/convert/parse")
    public R<?> parseFile(@RequestParam("file") MultipartFile file) {
        String content = FileParseUtil.parseToText(file);
        return R.ok(content);
    }

    @Operation(summary = "规则解析")
    @PostMapping("/convert/rule-parse")
    public R<?> ruleParse(@RequestBody String content) {
        return R.ok(aiConvertService.parseByRule(content));
    }
}
