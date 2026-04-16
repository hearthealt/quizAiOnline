package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.dto.admin.QuestionDTO;
import com.quiz.service.QuestionConvertService;
import com.quiz.service.QuestionService;
import com.quiz.util.ExcelUtil;
import com.quiz.vo.admin.QuestionDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "题目管理")
@RestController
@RequestMapping("/api/admin/question")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionService questionService;
    private final QuestionConvertService questionConvertService;

    @Operation(summary = "题目分页列表")
    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) Long bankId,
                     @RequestParam(required = false) Integer type,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(defaultValue = "1") Integer pageNum,
                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(questionService.pageList(bankId, type, keyword, pageNum, pageSize));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public R<QuestionDetailVO> detail(@PathVariable Long id) {
        return R.ok(questionService.getDetail(id));
    }

    @Operation(summary = "创建题目")
    @PostMapping
    public R<Void> create(@RequestBody QuestionDTO dto) {
        questionService.create(dto);
        return R.ok();
    }

    @Operation(summary = "更新题目")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody QuestionDTO dto) {
        questionService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量删除题目")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        questionService.batchDelete(ids);
        return R.ok();
    }

    @Operation(summary = "导入题目")
    @PostMapping("/import")
    public R<?> importQuestions(@RequestParam("file") MultipartFile file,
                                @RequestParam(required = false) Long bankId,
                                @RequestParam(required = false) Long categoryId) throws IOException {
        return R.ok(questionService.importFromExcel(bankId, categoryId, file.getOriginalFilename(), file.getInputStream()));
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil.getTemplate(response);
    }

    // ==================== 题目转换相关 ====================

    @Operation(summary = "智能解析文件 - 自动识别格式并解析")
    @PostMapping("/convert/smart-parse")
    public R<?> smartParse(@RequestParam("file") MultipartFile file) {
        List<Map<String, Object>> questions = questionConvertService.parseSmart(file);
        return R.ok(Map.of(
                "mode", "smart",
                "questions", questions,
                "message", "解析完成，共 " + questions.size() + " 道题"
        ));
    }

    @Operation(summary = "将转换结果直接导入题库")
    @PostMapping("/convert/import")
    public R<?> importConverted(@RequestParam("bankId") Long bankId,
                                @RequestBody List<Map<String, Object>> questions) {
        QuestionService.QuestionImportResult result = questionService.importFromConverted(bankId, questions);
        return R.ok(result);
    }
}
