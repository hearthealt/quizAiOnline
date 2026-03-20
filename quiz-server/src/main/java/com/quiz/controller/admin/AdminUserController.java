package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.entity.AiChatMessage;
import com.quiz.service.AiChatService;
import com.quiz.service.RecordService;
import com.quiz.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final RecordService recordService;
    private final AiChatService aiChatService;

    @Operation(summary = "用户分页列表")
    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) String keyword,
                     @RequestParam(required = false) Integer status,
                     @RequestParam(defaultValue = "1") Integer pageNum,
                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(userService.list(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "用户答题记录")
    @GetMapping("/{id}/records")
    public R<?> records(@PathVariable Long id,
                        @RequestParam(defaultValue = "1") Integer pageNum,
                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(recordService.appRecordList(id, null, pageNum, pageSize));
    }
    
    @Operation(summary = "用户 AI 对话记录")
    @GetMapping("/{id}/chat-history")
    public R<List<AiChatMessage>> chatHistory(@PathVariable Long id) {
        return R.ok(aiChatService.getAllHistory(id));
    }

    @Operation(summary = "设置用户 VIP")
    @PutMapping("/{id}/vip")
    public R<Void> setVip(@PathVariable Long id, 
                          @RequestParam Integer isVip,
                          @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime expireTime) {
        userService.setVip(id, isVip, expireTime);
        return R.ok();
    }
}
