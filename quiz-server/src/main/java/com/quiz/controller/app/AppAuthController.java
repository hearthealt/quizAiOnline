package com.quiz.controller.app;

import com.quiz.common.result.R;
import com.quiz.dto.app.PhoneLoginDTO;
import com.quiz.dto.app.WxLoginDTO;
import com.quiz.service.WxAuthService;
import com.quiz.vo.app.LoginVO;
import com.quiz.vo.app.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "小程序-认证")
@RestController
@RequestMapping("/api/app/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final WxAuthService wxAuthService;

    @Operation(summary = "微信登录")
    @PostMapping("/wx-login")
    public R<LoginVO> wxLogin(@RequestBody WxLoginDTO dto) {
        return R.ok(wxAuthService.wxLogin(dto));
    }

    @Operation(summary = "手机号登录")
    @PostMapping("/phone-login")
    public R<LoginVO> phoneLogin(@RequestBody PhoneLoginDTO dto) {
        return R.ok(wxAuthService.phoneLogin(dto));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        wxAuthService.logout();
        return R.ok();
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public R<UserInfoVO> getInfo() {
        return R.ok(wxAuthService.getInfo());
    }
}
