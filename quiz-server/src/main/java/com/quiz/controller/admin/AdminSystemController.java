package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.dto.admin.AdminCreateDTO;
import com.quiz.dto.admin.AdminUpdateDTO;
import com.quiz.service.AdminService;
import com.quiz.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Tag(name = "系统管理")
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final AdminService adminService;
    private final SysConfigService sysConfigService;

    /** 后台系统设置允许管理的配置项 */
    private static final Set<String> ADMIN_KEYS = Set.copyOf(Arrays.asList(
            "siteName", "siteDescription", "siteLogo", "copyright", "icpNumber",
            "practiceManagerContact",
            "wxAppId", "wxAppSecret",
            "registerEnabled",
            "aiChatPersona", "aiChatGreeting",
            "aiPromptAnalysis", "aiPromptAnswer", "aiPromptBoth"
    ));

    @Operation(summary = "管理员列表")
    @GetMapping("/admin/list")
    public R<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.list(pageNum, pageSize));
    }

    @Operation(summary = "创建管理员")
    @PostMapping("/admin")
    public R<Void> create(@RequestBody AdminCreateDTO dto) {
        adminService.create(dto);
        return R.ok();
    }

    @Operation(summary = "更新管理员")
    @PutMapping("/admin/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody AdminUpdateDTO dto) {
        adminService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除管理员")
    @DeleteMapping("/admin/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminService.delete(id);
        return R.ok();
    }

    @Operation(summary = "获取系统配置")
    @GetMapping("/setting")
    public R<?> getSetting() {
        Map<String, String> all = sysConfigService.getAll();
        Map<String, String> filtered = new LinkedHashMap<>();
        for (String key : ADMIN_KEYS) {
            if (all.containsKey(key)) {
                filtered.put(key, all.get(key));
            }
        }
        return R.ok(filtered);
    }

    @Operation(summary = "更新系统配置")
    @PutMapping("/setting")
    public R<Void> updateSetting(@RequestBody Map<String, String> configs) {
        if (configs != null && !configs.isEmpty()) {
            Map<String, String> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                if (ADMIN_KEYS.contains(entry.getKey())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            if (!filtered.isEmpty()) {
                sysConfigService.batchUpdate(filtered);
            }
        }
        return R.ok();
    }
}
