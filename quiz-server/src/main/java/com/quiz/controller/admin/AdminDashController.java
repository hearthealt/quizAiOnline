package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.service.StatisticsService;
import com.quiz.vo.admin.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashController {

    private final StatisticsService statisticsService;

    @Operation(summary = "获取总览数据")
    @GetMapping("/overview")
    public R<DashboardVO> overview() {
        return R.ok(statisticsService.getDashboardOverview());
    }

    @Operation(summary = "获取趋势数据")
    @GetMapping("/trend")
    public R<?> trend(@RequestParam(defaultValue = "7") Integer days) {
        return R.ok(statisticsService.getDashboardTrend(days));
    }

    @Operation(summary = "获取题库热度排行")
    @GetMapping("/rank")
    public R<?> rank(@RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(statisticsService.getBankHotRank(limit));
    }
}
