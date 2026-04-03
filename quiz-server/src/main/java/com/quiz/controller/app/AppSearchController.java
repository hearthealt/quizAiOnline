package com.quiz.controller.app;

import com.quiz.common.result.PageResult;
import com.quiz.common.result.R;
import com.quiz.service.QuestionService;
import com.quiz.vo.app.QuestionListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "小程序-搜索")
@RestController
@RequestMapping("/api/app/search")
@RequiredArgsConstructor
public class AppSearchController {

    private final QuestionService questionService;

    @Operation(summary = "搜索题目")
    @GetMapping
    public R<PageResult<QuestionListVO>> search(@RequestParam String keyword,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(questionService.pageAppList(null, keyword, pageNum, pageSize));
    }

    @Operation(summary = "热门搜索")
    @GetMapping("/hot")
    public R<List<String>> hot() {
        return R.ok(List.of());
    }
}
