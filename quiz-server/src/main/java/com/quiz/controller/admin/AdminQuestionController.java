package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.common.constant.CommonConstant;
import com.quiz.config.StpKit;
import com.quiz.dto.admin.QuestionDTO;
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

@Tag(name = "题目管理")
@RestController
@RequestMapping("/api/admin/question")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionService questionService;

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
        if (isSuperAdmin()) {
            questionService.create(dto);
        } else {
            if (dto.getStatus() != null) {
                StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
            }
            dto.setStatus(0);
            questionService.create(dto);
        }
        return R.ok();
    }

    private boolean isSuperAdmin() {
        return StpKit.ADMIN.hasRole(CommonConstant.ROLE_SUPER_ADMIN);
    }

    @Operation(summary = "更新题目")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody QuestionDTO dto) {
        if (dto.getStatus() != null) {
            StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        }
        questionService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "更新题目状态")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        questionService.toggleStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "批量更新题目状态")
    @PutMapping("/batch/status")
    public R<Void> batchToggleStatus(@RequestBody List<Long> ids, @RequestParam Integer status) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        questionService.batchToggleStatus(ids, status);
        return R.ok();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        questionService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量删除题目")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
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
}
