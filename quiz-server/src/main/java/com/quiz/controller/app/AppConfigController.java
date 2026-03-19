package com.quiz.controller.app;

import com.quiz.common.result.R;
import com.quiz.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "小程序-系统配置")
@RestController
@RequestMapping("/api/app/config")
@RequiredArgsConstructor
public class AppConfigController {

    private final SysConfigService sysConfigService;

    /** 允许小程序读取的配置项（不含敏感信息） */
    private static final List<String> PUBLIC_KEYS = Arrays.asList(
            "siteName", "siteDescription", "siteLogo", "copyright", "icpNumber",
            "registerEnabled",
            "aiChatGreeting"
    );

    @Operation(summary = "获取公开系统配置")
    @GetMapping
    public R<?> getPublicConfig() {
        Map<String, String> all = sysConfigService.getAll();
        Map<String, String> publicConfig = new LinkedHashMap<>();
        for (String key : PUBLIC_KEYS) {
            if (all.containsKey(key)) {
                publicConfig.put(key, all.get(key));
            }
        }
        return R.ok(publicConfig);
    }
}
