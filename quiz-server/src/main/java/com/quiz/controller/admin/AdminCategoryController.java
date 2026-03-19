package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.dto.admin.CategoryDTO;
import com.quiz.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
        categoryService.create(dto);
        return R.ok();
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody CategoryDTO dto) {
        categoryService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }

    @Operation(summary = "切换分类状态")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        categoryService.toggleStatus(id, status);
        return R.ok();
    }
}
