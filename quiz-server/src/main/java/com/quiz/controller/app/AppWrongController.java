package com.quiz.controller.app;

import com.quiz.common.result.PageResult;
import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.service.WrongQuestionService;
import com.quiz.vo.app.WrongQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "小程序-错题")
@RestController
@RequestMapping("/api/app/wrong")
@RequiredArgsConstructor
public class AppWrongController {

    private final WrongQuestionService wrongQuestionService;

    @Operation(summary = "错题列表")
    @GetMapping("/list")
    public R<PageResult<WrongQuestionVO>> list(@RequestParam(required = false) Long bankId,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(wrongQuestionService.list(userId, bankId, pageNum, pageSize));
    }

    @Operation(summary = "错题统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(wrongQuestionService.stats(userId));
    }

    @Operation(summary = "删除错题")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        wrongQuestionService.remove(userId, id);
        return R.ok();
    }
}
