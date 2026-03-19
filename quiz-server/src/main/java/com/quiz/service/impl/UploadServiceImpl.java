package com.quiz.service.impl;

import com.quiz.common.exception.BizException;
import com.quiz.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final String ALLOWED_FILE_TYPES = "jpg,jpeg,png,gif,webp,svg,ico";
    private static final long MAX_FILE_SIZE_MB = 10;

    @Value("${file.upload-path:./uploads/}")
    private String defaultUploadPath;

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BizException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String allowedTypes = ALLOWED_FILE_TYPES;
        String extLower = ext.toLowerCase().replace(".", "");
        String[] allowed = allowedTypes.split(",");
        boolean valid = false;
        for (String a : allowed) {
            if (a.trim().equalsIgnoreCase(extLower)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new BizException("只支持 " + allowedTypes + " 格式");
        }

        long maxSize = MAX_FILE_SIZE_MB * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BizException("文件大小不能超过 " + MAX_FILE_SIZE_MB + "MB");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        File dir = new File(defaultUploadPath).getAbsoluteFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dest = new File(dir, filename);
        file.transferTo(dest.getAbsoluteFile());

        return "/uploads/" + filename;
    }
}
