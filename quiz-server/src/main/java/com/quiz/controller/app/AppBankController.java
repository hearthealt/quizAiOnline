package com.quiz.controller.app;

import com.quiz.common.enums.BizCode;
import com.quiz.common.result.PageResult;
import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.entity.QuestionBank;
import com.quiz.service.QuestionBankService;
import com.quiz.service.QuestionService;
import com.quiz.vo.app.BankDetailVO;
import com.quiz.vo.app.QuestionListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小程序-题库")
@RestController
@RequestMapping("/api/app/bank")
@RequiredArgsConstructor
public class AppBankController {

    private final QuestionBankService bankService;
    private final QuestionService questionService;

    @Operation(summary = "题库列表")
    @GetMapping("/list")
    public R<PageResult<QuestionBank>> list(@RequestParam(required = false) Long categoryId,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(bankService.pageEnabledList(categoryId, pageNum, pageSize));
    }

    @Operation(summary = "题库详情")
    @GetMapping("/{id}")
    public R<BankDetailVO> detail(@PathVariable Long id) {
        QuestionBank bank = bankService.getById(id);
        if (bank == null || bank.getStatus() == null || bank.getStatus() != 1) {
            throw BizCode.BANK_NOT_FOUND.exception();
        }
        Long userId = StpKit.APP.isLogin() ? StpKit.APP.getLoginIdAsLong() : null;
        return R.ok(bankService.getDetail(id, userId));
    }

    @Operation(summary = "题库下的题目列表")
    @GetMapping("/{id}/questions")
    public R<PageResult<QuestionListVO>> questions(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        QuestionBank bank = bankService.getById(id);
        if (bank == null || bank.getStatus() == null || bank.getStatus() != 1) {
            throw BizCode.BANK_NOT_FOUND.exception();
        }
        return R.ok(questionService.pageAppList(id, null, pageNum, pageSize));
    }

    @Operation(summary = "热门题库")
    @GetMapping("/hot")
    public R<List<QuestionBank>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(bankService.hotBanks(limit));
    }
}
