package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.service.WrongQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "错题管理")
@RestController
@RequestMapping("/api/admin/wrong")
@RequiredArgsConstructor
public class AdminWrongController {

    private final WrongQuestionService wrongQuestionService;

    @Operation(summary = "错题列表")
    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) String keyword,
                     @RequestParam(required = false) Long bankId,
                     @RequestParam(defaultValue = "1") Integer pageNum,
                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(wrongQuestionService.adminList(keyword, bankId, pageNum, pageSize));
    }
}
