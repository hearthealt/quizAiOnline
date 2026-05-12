package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.dto.admin.CategoryDTO;
import com.quiz.common.constant.CommonConstant;
import com.quiz.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "分类管理")
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类分页列表")
    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) String keyword,
                     @RequestParam(defaultValue = "1") Integer pageNum,
                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(categoryService.list(keyword, pageNum, pageSize));
    }

    @Operation(summary = "全部分类")
    @GetMapping("/all")
    public R<?> listAll() {
        return R.ok(categoryService.listAll());
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public R<Void> create(@RequestBody CategoryDTO dto) {
        if (isSuperAdmin()) {
            categoryService.create(dto);
        } else {
            if (dto.getStatus() != null) {
                StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
            }
            dto.setStatus(0);
            categoryService.create(dto);
        }
        return R.ok();
    }

    private boolean isSuperAdmin() {
        return StpKit.ADMIN.hasRole(CommonConstant.ROLE_SUPER_ADMIN);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody CategoryDTO dto) {
        if (dto.getStatus() != null) {
            StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        }
        categoryService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        categoryService.delete(id);
        return R.ok();
    }

    @Operation(summary = "切换分类状态")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        categoryService.toggleStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "批量切换分类状态")
    @PutMapping("/batch/status")
    public R<Void> batchToggleStatus(@RequestBody List<Long> ids, @RequestParam Integer status) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        categoryService.batchToggleStatus(ids, status);
        return R.ok();
    }

    @Operation(summary = "批量删除分类")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        categoryService.batchDelete(ids);
        return R.ok();
    }
}
