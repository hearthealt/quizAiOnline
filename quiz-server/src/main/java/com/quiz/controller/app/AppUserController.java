package com.quiz.controller.app;

import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.dto.app.UpdateProfileDTO;
import com.quiz.service.UserService;
import com.quiz.vo.app.StudyStatsVO;
import com.quiz.vo.app.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "小程序-用户")
@RestController
@RequestMapping("/api/app/user")
@RequiredArgsConstructor
public class AppUserController {

    private final UserService userService;

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public R<UserInfoVO> profile() {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(userService.getUserInfo(userId));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody UpdateProfileDTO dto) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        userService.updateProfile(userId, dto);
        return R.ok();
    }

    @Operation(summary = "学习统计")
    @GetMapping("/study-stats")
    public R<StudyStatsVO> studyStats() {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(userService.getStudyStats(userId));
    }

    @Operation(summary = "更新设置")
    @PutMapping("/settings")
    public R<Void> updateSettings(@RequestBody String settingsJson) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        userService.updateSettings(userId, settingsJson);
        return R.ok();
    }
}
