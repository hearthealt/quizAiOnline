package com.quiz.controller.app;

import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.dto.app.AiChatDTO;
import com.quiz.entity.AiChatMessage;
import com.quiz.service.AiChatService;
import com.quiz.vo.app.AiChatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小程序-AI辅导")
@RestController
@RequestMapping("/api/app/ai")
@RequiredArgsConstructor
public class AppAiController {

    private final AiChatService aiChatService;

    @Operation(summary = "AI对话")
    @PostMapping("/chat")
    public R<AiChatVO> chat(@RequestBody AiChatDTO dto) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        String reply = aiChatService.chat(dto, userId);
        AiChatVO vo = new AiChatVO();
        vo.setReply(reply);
        return R.ok(vo);
    }
    
    @Operation(summary = "获取对话历史")
    @GetMapping("/history")
    public R<List<AiChatMessage>> getHistory() {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(aiChatService.getHistory(userId));
    }
    
    @Operation(summary = "清空对话历史")
    @DeleteMapping("/history")
    public R<Void> clearHistory() {
        Long userId = StpKit.APP.getLoginIdAsLong();
        aiChatService.clearHistory(userId);
        return R.ok();
    }
}
