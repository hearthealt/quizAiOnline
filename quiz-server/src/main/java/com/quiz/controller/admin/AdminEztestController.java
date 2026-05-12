package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.common.constant.CommonConstant;
import com.quiz.config.StpKit;
import com.quiz.dto.admin.EztestExportDTO;
import com.quiz.dto.admin.EztestJobImportDTO;
import com.quiz.dto.admin.EztestProfileDTO;
import com.quiz.dto.admin.EztestSessionQueryDTO;
import com.quiz.service.EztestService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/eztest")
@RequiredArgsConstructor
public class AdminEztestController {

    private final EztestService eztestService;

    @GetMapping("/profile")
    public R<?> profile() {
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        return R.ok(eztestService.getProfile(adminId));
    }

    @PutMapping("/profile")
    public R<?> saveProfile(@RequestBody EztestProfileDTO dto) {
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        return R.ok(eztestService.saveProfile(dto, adminId));
    }

    @PostMapping("/sessions")
    public R<?> sessions(@RequestBody EztestSessionQueryDTO dto) {
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        return R.ok(eztestService.listSessions(dto, adminId));
    }

    @PostMapping("/jobs")
    public R<?> createJob(@RequestBody EztestExportDTO dto) {
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        return R.ok(eztestService.createJob(dto, adminId));
    }

    @PostMapping("/jobs/{id}/import")
    public R<?> importJob(@PathVariable Long id, @RequestBody EztestJobImportDTO dto) {
        return R.ok(eztestService.importJob(id, dto));
    }

    @GetMapping("/jobs/{id}")
    public R<?> job(@PathVariable Long id) {
        return R.ok(eztestService.getJob(id));
    }

    @GetMapping("/jobs/{id}/files")
    public R<?> jobFiles(@PathVariable Long id,
                         @RequestParam(defaultValue = "1") Integer pageNum,
                         @RequestParam(defaultValue = "5") Integer pageSize) {
        return R.ok(eztestService.getJobFiles(id, pageNum, pageSize));
    }

    @GetMapping("/jobs")
    public R<?> jobList(@RequestParam(defaultValue = "1") Integer pageNum,
                        @RequestParam(defaultValue = "10") Integer pageSize,
                        @RequestParam(required = false) Integer status) {
        return R.ok(eztestService.getJobList(pageNum, pageSize, status));
    }

    @GetMapping("/jobs/{jobId}/files/{fileId}")
    public void downloadFile(@PathVariable Long jobId,
                             @PathVariable Long fileId,
                             HttpServletResponse response) throws IOException {
        eztestService.downloadFile(jobId, fileId, response);
    }

    @DeleteMapping("/jobs/{id}")
    public R<Void> deleteJob(@PathVariable Long id) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        eztestService.deleteJob(id);
        return R.ok();
    }

    @DeleteMapping("/jobs/batch")
    public R<Void> batchDeleteJobs(@RequestBody List<Long> ids) {
        StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN);
        eztestService.batchDeleteJobs(ids);
        return R.ok();
    }
}
