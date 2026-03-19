package com.quiz.controller.app;

import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.dto.app.StartPracticeDTO;
import com.quiz.dto.app.SubmitAnswerDTO;
import com.quiz.entity.PracticeRecord;
import com.quiz.service.PracticeService;
import com.quiz.vo.app.PracticeResultVO;
import com.quiz.vo.app.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "小程序-练习")
@RestController
@RequestMapping("/api/app/practice")
@RequiredArgsConstructor
public class AppPracticeController {

    private final PracticeService practiceService;

    @Operation(summary = "开始练习")
    @PostMapping("/start")
    public R<PracticeRecord> start(@RequestBody StartPracticeDTO dto) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(practiceService.startPractice(userId, dto));
    }

    @Operation(summary = "获取练习题目")
    @GetMapping("/{id}/question")
    public R<QuestionVO> getQuestion(@PathVariable Long id,
                                     @RequestParam Integer index) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(practiceService.getQuestion(id, index, userId));
    }

    @Operation(summary = "提交答案")
    @PostMapping("/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id, @RequestBody SubmitAnswerDTO dto) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(practiceService.submitAnswer(id, dto, userId));
    }

    @Operation(summary = "结束练习")
    @PostMapping("/{id}/finish")
    public R<PracticeResultVO> finish(@PathVariable Long id) {
        return R.ok(practiceService.finishPractice(id));
    }

    @Operation(summary = "获取练习进度")
    @GetMapping("/{id}/progress")
    public R<Map<String, Object>> progress(@PathVariable Long id) {
        return R.ok(practiceService.getProgress(id));
    }
}
