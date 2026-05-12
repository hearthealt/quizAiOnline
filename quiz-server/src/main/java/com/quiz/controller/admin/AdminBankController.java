package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.common.constant.CommonConstant;
import com.quiz.config.StpKit;
import com.quiz.dto.admin.QuestionBankDTO;
import com.quiz.service.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "题库管理")
@RestController
@RequestMapping("/api/admin/bank")
@RequiredArgsConstructor
public class AdminBankController {

    private final QuestionBankService questionBankService;

    @Operation(summary = "题库分页列表")
    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) Long categoryId,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(defaultValue = "1") Integer pageNum,
                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(questionBankService.pageList(categoryId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "题库详情")
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(questionBankService.getDetail(id, null));
    }

    @Operation(summary = "创建题库")
    @PostMapping
    public R<Void> create(@RequestBody QuestionBankDTO dto) {
        if (isSuperAdmin()) {
            questionBankService.create(dto);
        } else {
            if (dto.getStatus() != null) {
                StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
            }
            dto.setStatus(0);
            questionBankService.create(dto);
        }
        return R.ok();
    }

    private boolean isSuperAdmin() {
        return StpKit.ADMIN.hasRole(CommonConstant.ROLE_SUPER_ADMIN);
    }

    @Operation(summary = "更新题库")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody QuestionBankDTO dto) {
        if (dto.getStatus() != null) {
            StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        }
        questionBankService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "更新题库状态")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        questionBankService.toggleStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "批量更新题库状态")
    @PutMapping("/batch/status")
    public R<Void> batchToggleStatus(@RequestBody List<Long> ids, @RequestParam Integer status) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        questionBankService.batchToggleStatus(ids, status);
        return R.ok();
    }

    @Operation(summary = "删除题库")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        questionBankService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量删除题库")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        questionBankService.batchDelete(ids);
        return R.ok();
    }
}
