package com.quiz.controller.app;

import com.quiz.common.result.PageResult;
import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.dto.app.SaveExamAnswerDTO;
import com.quiz.dto.app.StartExamDTO;
import com.quiz.dto.app.SubmitExamDTO;
import com.quiz.service.ExamService;
import com.quiz.vo.app.ExamOngoingVO;
import com.quiz.vo.app.ExamResultVO;
import com.quiz.vo.app.ExamSessionVO;
import com.quiz.vo.app.RecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@Tag(name = "小程序-考试")
@RestController
@RequestMapping("/api/app/exam")
@RequiredArgsConstructor
public class AppExamController {

    private final ExamService examService;

    @Operation(summary = "开始考试")
    @PostMapping("/start")
    public R<ExamSessionVO> start(@RequestBody StartExamDTO dto) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(examService.startExam(userId, dto));
    }

    @Operation(summary = "获取进行中的考试")
    @GetMapping("/ongoing")
    public R<ExamOngoingVO> ongoing(@RequestParam Long bankId) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(examService.getOngoingExam(userId, bankId));
    }

    @Operation(summary = "继续考试会话")
    @GetMapping("/{id}/session")
    public R<ExamSessionVO> session(@PathVariable Long id) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(examService.getExamSession(userId, id));
    }

    @Operation(summary = "保存考试答案")
    @PostMapping("/{id}/answer")
    public R<Boolean> saveAnswer(@PathVariable Long id, @RequestBody SaveExamAnswerDTO dto) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        examService.saveExamAnswer(userId, id, dto);
        return R.ok(true);
    }

    @Operation(summary = "提交考试")
    @PostMapping("/{id}/submit")
    public R<ExamResultVO> submit(@PathVariable Long id, @RequestBody SubmitExamDTO dto) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(examService.submitExam(id, dto, userId));
    }

    @Operation(summary = "考试结果")
    @GetMapping("/{id}/result")
    public R<ExamResultVO> result(@PathVariable Long id) {
        return R.ok(examService.getResult(id));
    }

    @Operation(summary = "考试记录")
    @GetMapping("/records")
    public R<PageResult<RecordVO>> records(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(examService.getRecords(userId, pageNum, pageSize));
    }
}
