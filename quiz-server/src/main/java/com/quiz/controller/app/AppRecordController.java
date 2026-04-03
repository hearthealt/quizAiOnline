package com.quiz.controller.app;

import com.quiz.common.result.PageResult;
import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.service.RecordService;
import com.quiz.vo.app.RecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "小程序-记录")
@RestController
@RequestMapping("/api/app/record")
@RequiredArgsConstructor
public class AppRecordController {

    private final RecordService recordService;

    @Operation(summary = "记录列表")
    @GetMapping("/list")
    public R<PageResult<RecordVO>> list(@RequestParam(required = false) String type,
                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(recordService.appRecordList(userId, type, pageNum, pageSize));
    }

    @Operation(summary = "记录详情")
    @GetMapping("/{id}/detail")
    public R<Map<String, Object>> detail(@PathVariable Long id,
                                          @RequestParam String type) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(recordService.appRecordDetail(id, type, userId));
    }
}
