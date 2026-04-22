package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "活跃分析")
@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
public class AdminActivityController {

    private final ActivityService activityService;

    @Operation(summary = "活跃概览")
    @GetMapping("/overview")
    public R<?> overview(@RequestParam(required = false) String date) {
        return R.ok(activityService.getOverview(date));
    }

    @Operation(summary = "活跃用户列表")
    @GetMapping("/users")
    public R<?> users(@RequestParam(required = false) String date,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(defaultValue = "1") Integer pageNum,
                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(activityService.getActiveUsers(date, keyword, pageNum, pageSize));
    }

    @Operation(summary = "用户当日行为详情")
    @GetMapping("/users/{userId}")
    public R<?> userDetail(@PathVariable Long userId,
                           @RequestParam(required = false) String date,
                           @RequestParam(defaultValue = "1") Integer timelinePageNum,
                           @RequestParam(defaultValue = "10") Integer timelinePageSize,
                           @RequestParam(defaultValue = "1") Integer answerPageNum,
                           @RequestParam(defaultValue = "10") Integer answerPageSize) {
        return R.ok(activityService.getUserActivityDetail(
                userId,
                date,
                timelinePageNum,
                timelinePageSize,
                answerPageNum,
                answerPageSize
        ));
    }

    @Operation(summary = "答题明细")
    @GetMapping("/answers")
    public R<?> answers(@RequestParam(required = false) String date,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long bankId,
                        @RequestParam(required = false) String source,
                        @RequestParam(required = false) String result,
                        @RequestParam(defaultValue = "1") Integer pageNum,
                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(activityService.getAnswerDetails(date, keyword, bankId, source, result, pageNum, pageSize));
    }
}
