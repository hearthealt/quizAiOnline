package com.quiz.controller.app;

import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "小程序-文件上传")
@RestController
@RequestMapping("/api/app/upload")
@RequiredArgsConstructor
public class AppUploadController {

    private final UploadService uploadService;

    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        StpKit.APP.checkLogin();
        String url = uploadService.uploadImage(file);
        return R.ok(url);
    }
}
