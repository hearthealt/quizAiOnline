package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.dto.admin.AdminLoginDTO;
import com.quiz.dto.admin.ChangePasswordDTO;
import com.quiz.service.AdminService;
import com.quiz.vo.admin.AdminInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员认证")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public R<AdminInfoVO> login(@RequestBody AdminLoginDTO dto) {
        return R.ok(adminService.login(dto));
    }

    @Operation(summary = "管理员登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpKit.ADMIN.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/info")
    public R<AdminInfoVO> info() {
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        return R.ok(adminService.getInfo(adminId));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        adminService.changePassword(adminId, dto);
        return R.ok();
    }
}
