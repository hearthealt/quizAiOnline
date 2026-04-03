package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "答题记录管理")
@RestController
@RequestMapping("/api/admin/record")
@RequiredArgsConstructor
public class AdminRecordController {

    private final RecordService recordService;

    @Operation(summary = "练习记录列表")
    @GetMapping("/practice/list")
    public R<?> practiceList(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) Long bankId,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(recordService.adminPracticeRecords(keyword, bankId, pageNum, pageSize));
    }

    @Operation(summary = "练习记录详情")
    @GetMapping("/practice/{id}")
    public R<?> practiceDetail(@PathVariable Long id) {
        return R.ok(recordService.appRecordDetail(id, "practice", null));
    }

    @Operation(summary = "考试记录列表")
    @GetMapping("/exam/list")
    public R<?> examList(@RequestParam(required = false) String keyword,
                         @RequestParam(required = false) Long bankId,
                         @RequestParam(defaultValue = "1") Integer pageNum,
                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(recordService.adminExamRecords(keyword, bankId, pageNum, pageSize));
    }

    @Operation(summary = "考试记录详情")
    @GetMapping("/exam/{id}")
    public R<?> examDetail(@PathVariable Long id) {
        return R.ok(recordService.appRecordDetail(id, "exam", null));
    }
}
