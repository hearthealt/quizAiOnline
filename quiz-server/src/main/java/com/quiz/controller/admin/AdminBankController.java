package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.dto.admin.QuestionBankDTO;
import com.quiz.service.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
        questionBankService.create(dto);
        return R.ok();
    }

    @Operation(summary = "更新题库")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody QuestionBankDTO dto) {
        questionBankService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除题库")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        questionBankService.delete(id);
        return R.ok();
    }
}
