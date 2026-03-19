package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "统计分析")
@RestController
@RequestMapping("/api/admin/stat")
@RequiredArgsConstructor
public class AdminStatController {

    private final StatisticsService statisticsService;

    @Operation(summary = "用户增长趋势")
    @GetMapping("/user-growth")
    public R<?> userGrowth(@RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate) {
        return R.ok(statisticsService.getUserGrowth(startDate, endDate));
    }

    @Operation(summary = "答题趋势")
    @GetMapping("/answer-trend")
    public R<?> answerTrend(@RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate) {
        return R.ok(statisticsService.getAnswerTrend(startDate, endDate));
    }

    @Operation(summary = "题库热度排行")
    @GetMapping("/bank-hot")
    public R<?> bankHot(@RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(statisticsService.getBankHotRank(limit));
    }

    @Operation(summary = "易错题排行")
    @GetMapping("/wrong-rank")
    public R<?> wrongRank(@RequestParam(required = false) Long bankId,
                          @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(statisticsService.getWrongRank(bankId, limit));
    }

    @Operation(summary = "正确率分布")
    @GetMapping("/accuracy-dist")
    public R<?> accuracyDist(@RequestParam(required = false) Long bankId) {
        return R.ok(statisticsService.getAccuracyDist(bankId));
    }

    @Operation(summary = "收藏排行")
    @GetMapping("/favorite-rank")
    public R<?> favoriteRank(@RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(statisticsService.getFavoriteRank(limit));
    }
}
