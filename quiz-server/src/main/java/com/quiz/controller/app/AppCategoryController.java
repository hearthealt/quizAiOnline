package com.quiz.controller.app;

import com.quiz.common.result.PageResult;
import com.quiz.common.result.R;
import com.quiz.entity.Category;
import com.quiz.entity.QuestionBank;
import com.quiz.service.CategoryService;
import com.quiz.service.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小程序-分类")
@RestController
@RequestMapping("/api/app/category")
@RequiredArgsConstructor
public class AppCategoryController {

    private final CategoryService categoryService;
    private final QuestionBankService bankService;

    @Operation(summary = "分类列表")
    @GetMapping("/list")
    public R<List<Category>> list() {
        return R.ok(categoryService.listAll());
    }

    @Operation(summary = "分类下的题库")
    @GetMapping("/{id}/banks")
    public R<PageResult<QuestionBank>> banks(@PathVariable Long id,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(bankService.pageList(id, pageNum, pageSize));
    }
}
