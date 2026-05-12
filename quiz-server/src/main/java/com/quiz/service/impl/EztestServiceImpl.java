package com.quiz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.EztestExportDTO;
import com.quiz.dto.admin.EztestJobImportDTO;
import com.quiz.dto.admin.EztestProfileDTO;
import com.quiz.dto.admin.EztestSessionQueryDTO;
import com.quiz.entity.Admin;
import com.quiz.entity.EztestJob;
import com.quiz.entity.EztestJobFile;
import com.quiz.entity.EztestProfile;
import com.quiz.mapper.AdminMapper;
import com.quiz.mapper.EztestJobFileMapper;
import com.quiz.mapper.EztestJobMapper;
import com.quiz.mapper.EztestProfileMapper;
import com.quiz.service.EztestService;
import com.quiz.service.QuestionService;
import com.quiz.vo.admin.EztestExportSubmitVO;
import com.quiz.vo.admin.EztestJobFileVO;
import com.quiz.vo.admin.EztestJobVO;
import com.quiz.vo.admin.EztestProfileVO;
import com.quiz.vo.admin.EztestSessionVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EztestServiceImpl implements EztestService, ApplicationRunner {

    private static final String DEFAULT_BASE_URL = "https://eztest.org";
    private static final String DEFAULT_EXAM_ID = "104748";
    private static final String DEFAULT_LAN = "zh";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0";
    private static final String FILE_TYPE_XLSX = "XLSX";
    private static final String FILE_TYPE_PDF_WITH_ANSWERS = "PDF_WITH_ANSWERS";
    private static final String FILE_TYPE_PDF_WITHOUT_ANSWERS = "PDF_WITHOUT_ANSWERS";
    private static final int JOB_PENDING = 0;
    private static final int JOB_RUNNING = 1;
    private static final int JOB_SUCCESS = 2;
    private static final int JOB_PARTIAL_FAILED = 3;
    private static final int JOB_FAILED = 4;
    private static final int PROGRESS_START = 3;
    private static final int PROGRESS_EXPORT_END = 64;
    private static final int PROGRESS_CONVERTED = 72;
    private static final int PROGRESS_FILES_END = 88;
    private static final int PROGRESS_IMPORTING = 92;
    private static final int PROGRESS_DONE = 100;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern BR_PATTERN = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_END_PATTERN = Pattern.compile("</p\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANSWER_BLANK_PATTERN = Pattern.compile("（[\\s\\u3000]*）|\\([\\s\\u3000]*\\)");
    private static final Pattern TRAILING_ANSWER_PATTERN = Pattern.compile(
            "[\\s\\u3000]*[（(]\\s*([A-Z0-9]+(?:\\s*[,，、/|]\\s*[A-Z0-9]+)*)\\s*[）)]\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TRAILING_SESSION_TIME_PATTERN = Pattern.compile(
            "[-\\s]*\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\s+\\d{1,2}[:：_]\\d{2}\\s*[~～\\-–—]\\s*\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\s+\\d{1,2}[:：_]\\d{2}$"
    );

    private static final List<String> SESSION_ID_FIELDS = List.of(
            "session_id", "sessionid", "sessionId", "united_session_id", "unitedSessionId", "entry_id", "entryId", "id"
    );
    private static final List<String> SESSION_NAME_FIELDS = List.of(
            "name", "title", "session_name", "sessionName", "exam_name", "paper_name"
    );
    private static final List<String> SESSION_STATUS_FIELDS = List.of("status", "entry_status", "entryStatus", "state");
    private static final List<String> SESSION_TIME_FIELDS = List.of("time", "exam_time", "examTime", "start_time", "startTime");
    private static final List<String> SESSION_START_FIELDS = List.of("start", "start_time", "startTime");
    private static final List<String> SESSION_END_FIELDS = List.of("end", "end_time", "endTime");
    private static final List<String> ENTRY_SESSIONID_FIELDS = List.of(
            "entry_sessionid", "entry_session_id", "entrySessionId", "session_cookie", "cookie"
    );

    private final EztestJobMapper eztestJobMapper;
    private final EztestJobFileMapper eztestJobFileMapper;
    private final EztestProfileMapper eztestProfileMapper;
    private final AdminMapper adminMapper;
    private final QuestionService questionService;
    private final OkHttpClient okHttpClient;
    private final ThreadPoolTaskExecutor aiTaskExecutor;

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    @Override
    public void run(ApplicationArguments args) {
        List<EztestJob> pendingJobs;
        try {
            pendingJobs = eztestJobMapper.selectPendingJobs();
        } catch (Exception e) {
            log.error("扫描未完成 EZTest 任务失败", e);
            return;
        }
        for (EztestJob pendingJob : pendingJobs) {
            eztestJobMapper.markFinished(
                    pendingJob.getId(),
                    JOB_FAILED,
                    "服务重启后未自动恢复，请重新提交任务",
                    "任务已中断",
                    appendLog(pendingJob.getLogs(), "服务重启，排队任务标记为失败"),
                    LocalDateTime.now()
            );
        }
    }

    @Override
    public EztestProfileVO getProfile(Long operatorId) {
        EztestProfile profile = operatorId == null ? null : eztestProfileMapper.selectByOperatorId(operatorId);
        return new EztestProfileVO(profile == null ? "" : nullToEmpty(profile.getPermit()));
    }

    @Override
    public EztestProfileVO saveProfile(EztestProfileDTO dto, Long operatorId) {
        if (operatorId == null) {
            throw new BizException("管理员未登录");
        }
        String permit = dto == null ? "" : normalizeText(dto.getPermit());
        savePermitProfile(operatorId, permit);
        return new EztestProfileVO(permit);
    }

    @Override
    public List<EztestSessionVO> listSessions(EztestSessionQueryDTO dto, Long operatorId) {
        if (dto == null || !hasText(dto.getPermit())) {
            throw new BizException("请输入准考证号或手机号");
        }
        String permit = dto.getPermit().trim();
        if (operatorId != null) {
            savePermitProfile(operatorId, permit);
        }
        CookieJar cookieJar = new CookieJar("");
        return fetchSessionCandidates(DEFAULT_EXAM_ID, permit, cookieJar);
    }

    @Override
    public EztestExportSubmitVO createJob(EztestExportDTO dto, Long operatorId) {
        if (dto == null) {
            throw new BizException("请求参数不能为空");
        }
        if (!hasText(dto.getPermit())) {
            throw new BizException("请输入准考证号或手机号");
        }
        String permit = dto.getPermit().trim();
        if (operatorId != null) {
            savePermitProfile(operatorId, permit);
        }
        if (dto.getSessionIds() == null || dto.getSessionIds().isEmpty()) {
            throw new BizException("请选择要导出的题库");
        }
        boolean exportXlsx = dto.getExportXlsx() == null || Boolean.TRUE.equals(dto.getExportXlsx());
        boolean exportPdfWithAnswers = Boolean.TRUE.equals(dto.getExportPdfWithAnswers());
        boolean exportPdfWithoutAnswers = Boolean.TRUE.equals(dto.getExportPdfWithoutAnswers());
        boolean importToBank = dto.getImportBankId() != null || dto.getImportCategoryId() != null;
        if (!exportXlsx && !exportPdfWithAnswers && !exportPdfWithoutAnswers && !importToBank) {
            throw new BizException("请至少选择一种导出产物或选择导入题库");
        }
        if (importToBank && dto.getImportBankId() == null && dto.getImportCategoryId() == null) {
            throw new BizException("未选择题库时，请先选择分类");
        }

        EztestJob job = new EztestJob();
        job.setPermitMasked(maskSensitive(permit));
        job.setSessionIds(joinDistinct(dto.getSessionIds()));
        job.setSessionNames("");
        job.setImportBankId(dto.getImportBankId());
        job.setImportCategoryId(dto.getImportCategoryId());
        job.setExportXlsx(exportXlsx ? 1 : 0);
        job.setExportPdfWithAnswers(exportPdfWithAnswers ? 1 : 0);
        job.setExportPdfWithoutAnswers(exportPdfWithoutAnswers ? 1 : 0);
        job.setSessionCount(splitCsv(job.getSessionIds()).size());
        job.setCompletedCount(0);
        job.setProgressPercent(0);
        job.setRawCount(0);
        job.setExportedCount(0);
        job.setDuplicateCount(0);
        job.setImportCreateCount(0);
        job.setImportUpdateCount(0);
        job.setImportFailCount(0);
        job.setStatus(JOB_PENDING);
        job.setProgressText("等待执行");
        job.setLogs(appendLog(null, "任务已提交"));
        job.setOperatorId(operatorId);
        eztestJobMapper.insert(job);

        EztestExportDTO taskDto = cloneExportDto(dto);
        taskDto.setPermit(permit);
        Long jobId = job.getId();
        aiTaskExecutor.execute(() -> runJob(jobId, taskDto));
        return new EztestExportSubmitVO(jobId, JOB_PENDING);
    }

    @Override
    @Transactional
    public EztestJobVO importJob(Long id, EztestJobImportDTO dto) {
        EztestJob job = requireJob(id);
        if (job.getStatus() != null && (job.getStatus() == JOB_PENDING || job.getStatus() == JOB_RUNNING)) {
            throw new BizException("执行中的任务不能导入题库");
        }
        if (!Objects.equals(job.getStatus(), JOB_SUCCESS) && !Objects.equals(job.getStatus(), JOB_PARTIAL_FAILED)) {
            throw new BizException("只有已完成任务可以导入题库");
        }
        if (hasImportResult(job)) {
            throw new BizException("该任务已导入题库，不能重复导入");
        }
        Long importBankId = dto == null ? null : dto.getImportBankId();
        Long importCategoryId = dto == null ? null : dto.getImportCategoryId();
        if (importBankId == null && importCategoryId == null) {
            throw new BizException("未选择题库时，请先选择分类");
        }
        List<ConvertedBundle> convertedBundles = parseImportPayload(job.getImportPayload());
        if (convertedBundles.isEmpty()) {
            throw new BizException("任务没有可导入题目，请重新导出");
        }

        QuestionService.QuestionImportResult importResult = importConvertedBundles(importBankId, importCategoryId, convertedBundles);
        eztestJobMapper.updateImportTarget(id, importBankId, importCategoryId);
        eztestJobMapper.updateImportCounts(
                id,
                importResult.getCreateCount(),
                importResult.getUpdateCount(),
                importResult.getFailCount()
        );
        String logs = appendLog(job.getLogs(), "手动导入完成：新增 " + importResult.getCreateCount()
                + " 题，更新 " + importResult.getUpdateCount()
                + " 题，失败 " + importResult.getFailCount() + " 题");
        int status = importResult.getFailCount() > 0 ? JOB_PARTIAL_FAILED : JOB_SUCCESS;
        eztestJobMapper.updateManualImportResult(id, status, importResult.getFailCount() > 0 ? "导入完成，部分题目失败" : "导入完成", logs);
        return getJob(id);
    }

    @Override
    public EztestJobVO getJob(Long id) {
        EztestJob job = requireJob(id);
        return toJobVO(job, eztestJobFileMapper.selectByJobId(id));
    }

    @Override
    public PageResult<EztestJobVO> getJobList(Integer pageNum, Integer pageSize, Integer status) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (safePageNum - 1) * safePageSize;
        List<EztestJob> jobs = eztestJobMapper.selectPage(offset, safePageSize, status);
        List<EztestJobVO> list = jobs.stream()
                .map(job -> toJobVO(job, eztestJobFileMapper.selectByJobId(job.getId())))
                .toList();
        return PageResult.of(list, eztestJobMapper.countAllJobs(status), safePageNum, safePageSize);
    }

    @Override
    public PageResult<EztestJobFileVO> getJobFiles(Long id, Integer pageNum, Integer pageSize) {
        requireJob(id);
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = 5;
        int offset = (safePageNum - 1) * safePageSize;
        List<EztestJobFileVO> list = eztestJobFileMapper.selectPageByJobId(id, offset, safePageSize)
                .stream()
                .map(this::toFileVO)
                .toList();
        return PageResult.of(list, eztestJobFileMapper.countByJobId(id), safePageNum, safePageSize);
    }

    @Override
    public void downloadFile(Long jobId, Long fileId, HttpServletResponse response) throws IOException {
        EztestJobFile file = eztestJobFileMapper.selectByIdAndJobId(fileId, jobId);
        if (file == null) {
            throw new BizException("文件不存在");
        }
        Path path = resolveStoredFilePath(file.getFilePath());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new BizException("文件已不存在");
        }
        String encoded = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encoded);
        response.setContentType(resolveContentType(file.getFileType()));
        response.setContentLengthLong(Files.size(path));
        try (InputStream input = Files.newInputStream(path)) {
            input.transferTo(response.getOutputStream());
        }
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        EztestJob job = requireJob(id);
        ensureJobCanBeDeleted(job);
        List<EztestJobFile> files = eztestJobFileMapper.selectByJobId(id);
        eztestJobFileMapper.deleteByJobId(id);
        int deleted = eztestJobMapper.deleteFinishedJob(id);
        if (deleted <= 0) {
            throw new BizException("任务删除失败");
        }
        for (EztestJobFile file : files) {
            try {
                Files.deleteIfExists(resolveStoredFilePath(file.getFilePath()));
            } catch (Exception e) {
                log.warn("删除 EZTest 产物失败: {}", file.getFilePath(), e);
            }
        }
        deleteJobOutputDir(id);
    }

    @Override
    @Transactional
    public void batchDeleteJobs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的任务");
        }
        List<Long> uniqueIds = ids.stream().distinct().toList();
        for (Long id : uniqueIds) {
            ensureJobCanBeDeleted(requireJob(id));
        }
        for (Long id : uniqueIds) {
            deleteJob(id);
        }
    }

    private void ensureJobCanBeDeleted(EztestJob job) {
        if (job == null) {
            throw new BizException("任务不存在");
        }
        if (job.getStatus() != null && (job.getStatus() == JOB_PENDING || job.getStatus() == JOB_RUNNING)) {
            throw new BizException("执行中的任务不能删除");
        }
    }

    private void runJob(Long jobId, EztestExportDTO dto) {
        EztestJob job = requireJob(jobId);
        String logs = job.getLogs();
        try {
            eztestJobMapper.markRunning(jobId, LocalDateTime.now(), "准备执行");
            logs = appendLog(logs, "开始执行任务");
            eztestJobMapper.updateStepProgress(jobId, PROGRESS_START, "准备读取题库", logs);
            CookieJar cookieJar = new CookieJar("");
            String permit = dto.getPermit().trim();
            List<String> requestedSessionIds = splitCsv(joinDistinct(dto.getSessionIds()));
            List<EztestSessionVO> availableSessions = fetchSessionCandidates(DEFAULT_EXAM_ID, permit, cookieJar);
            Map<String, EztestSessionVO> sessionById = availableSessions.stream()
                    .collect(Collectors.toMap(EztestSessionVO::getSessionId, item -> item, (a, b) -> a, LinkedHashMap::new));

            List<EztestSessionVO> selectedSessions = new ArrayList<>();
            for (String sessionId : requestedSessionIds) {
                EztestSessionVO selected = sessionById.get(sessionId);
                if (selected == null) {
                    selected = new EztestSessionVO();
                    selected.setSessionId(sessionId);
                    selected.setName("session " + sessionId);
                }
                selectedSessions.add(selected);
            }
            eztestJobMapper.updateSessions(
                    jobId,
                    selectedSessions.stream().map(EztestSessionVO::getName).collect(Collectors.joining(",")),
                    selectedSessions.size()
            );

            Path outputDir = resolveJobOutputDir(jobId);
            List<ExportBundle> bundles = new ArrayList<>();
            int completed = 0;
            int totalRaw = 0;
            int totalExported = 0;
            int totalDuplicate = 0;

            for (EztestSessionVO session : selectedSessions) {
                logs = appendLog(logs, "开始导出 " + safeSessionLabel(session));
                eztestJobMapper.updateProgress(jobId, completed, calculateExportProgress(completed, selectedSessions.size()), totalRaw, totalExported, totalDuplicate, "正在导出 " + safeSessionLabel(session), logs);
                ExportBundle bundle = exportSession(permit, session, outputDir, cookieJar);
                bundles.add(bundle);
                completed++;
                totalRaw += bundle.getRawCount();
                totalExported += bundle.getExportedCount();
                totalDuplicate += bundle.getDuplicateCount();
                logs = appendLog(logs, "完成导出 " + bundle.getExamName() + "，原始 " + bundle.getRawCount() + " 题，导出 " + bundle.getExportedCount() + " 题");
                eztestJobMapper.updateProgress(jobId, completed, calculateExportProgress(completed, selectedSessions.size()), totalRaw, totalExported, totalDuplicate, "已导出 " + completed + "/" + selectedSessions.size(), logs);
            }

            List<ConvertedBundle> convertedBundles = toConvertedBundles(bundles);
            eztestJobMapper.updateImportPayload(jobId, buildImportPayload(convertedBundles));
            eztestJobMapper.updateStepProgress(jobId, PROGRESS_CONVERTED, "题目转换完成", logs);

            boolean exportXlsx = dto.getExportXlsx() == null || Boolean.TRUE.equals(dto.getExportXlsx());
            boolean exportPdfWithAnswers = Boolean.TRUE.equals(dto.getExportPdfWithAnswers());
            boolean exportPdfWithoutAnswers = Boolean.TRUE.equals(dto.getExportPdfWithoutAnswers());
            int fileTotal = bundles.size() * ((exportXlsx ? 1 : 0) + (exportPdfWithAnswers ? 1 : 0) + (exportPdfWithoutAnswers ? 1 : 0));
            int fileDone = 0;
            for (ExportBundle bundle : bundles) {
                if (exportXlsx) {
                    eztestJobMapper.updateStepProgress(jobId, calculateFileProgress(fileDone, fileTotal), "正在生成 XLSX", logs);
                    Path xlsx = writeXlsx(bundle, outputDir);
                    saveFile(jobId, FILE_TYPE_XLSX, xlsx);
                    fileDone++;
                    logs = appendLog(logs, "已生成 XLSX：" + xlsx.getFileName());
                    eztestJobMapper.updateStepProgress(jobId, calculateFileProgress(fileDone, fileTotal), "已生成 " + fileDone + "/" + fileTotal + " 个文件", logs);
                }
                if (exportPdfWithAnswers) {
                    eztestJobMapper.updateStepProgress(jobId, calculateFileProgress(fileDone, fileTotal), "正在生成含答案 PDF", logs);
                    Path pdf = writePdf(bundle, outputDir, true);
                    saveFile(jobId, FILE_TYPE_PDF_WITH_ANSWERS, pdf);
                    fileDone++;
                    logs = appendLog(logs, "已生成含答案 PDF：" + pdf.getFileName());
                    eztestJobMapper.updateStepProgress(jobId, calculateFileProgress(fileDone, fileTotal), "已生成 " + fileDone + "/" + fileTotal + " 个文件", logs);
                }
                if (exportPdfWithoutAnswers) {
                    eztestJobMapper.updateStepProgress(jobId, calculateFileProgress(fileDone, fileTotal), "正在生成不含答案 PDF", logs);
                    Path pdf = writePdf(bundle, outputDir, false);
                    saveFile(jobId, FILE_TYPE_PDF_WITHOUT_ANSWERS, pdf);
                    fileDone++;
                    logs = appendLog(logs, "已生成不含答案 PDF：" + pdf.getFileName());
                    eztestJobMapper.updateStepProgress(jobId, calculateFileProgress(fileDone, fileTotal), "已生成 " + fileDone + "/" + fileTotal + " 个文件", logs);
                }
            }

            QuestionService.QuestionImportResult importResult = null;
            if (wantsImport(dto)) {
                eztestJobMapper.updateStepProgress(jobId, PROGRESS_IMPORTING, "正在导入题库", logs);
                importResult = importConvertedBundles(dto.getImportBankId(), dto.getImportCategoryId(), convertedBundles);
                eztestJobMapper.updateImportCounts(
                        jobId,
                        importResult.getCreateCount(),
                        importResult.getUpdateCount(),
                        importResult.getFailCount()
                );
                logs = appendLog(logs, "导入完成：新增 " + importResult.getCreateCount() + " 题，更新 " + importResult.getUpdateCount() + " 题，失败 " + importResult.getFailCount() + " 题");
                eztestJobMapper.updateStepProgress(jobId, PROGRESS_DONE, "导入完成", logs);
            }

            boolean partialFailed = importResult != null && importResult.getFailCount() > 0;
            eztestJobMapper.markFinished(
                    jobId,
                    partialFailed ? JOB_PARTIAL_FAILED : JOB_SUCCESS,
                    null,
                    partialFailed ? "导出完成，部分题目导入失败" : "任务完成",
                    logs,
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("EZTest 任务执行失败, jobId={}", jobId, e);
            eztestJobMapper.markFinished(
                    jobId,
                    JOB_FAILED,
                    abbreviate(e.getMessage(), 1000),
                    "任务失败",
                    appendLog(logs, "任务失败：" + e.getMessage()),
                    LocalDateTime.now()
            );
        }
    }

    private ExportBundle exportSession(String permit,
                                       EztestSessionVO session,
                                       Path outputDir,
                                       CookieJar baseCookieJar) {
        CookieJar cookieJar = new CookieJar(baseCookieJar.format());
        String sessionId = session.getSessionId();
        String baseUrl = DEFAULT_BASE_URL;
        String referer = baseUrl + "/exam/session/" + sessionId + "/";
        loginSelectedSession(permit, session, referer, cookieJar);

        JSONObject formPayload = fetchJson(
                buildApiUrl(baseUrl + "/dapi/exam/api/form/"),
                buildHeaders(referer, cookieJar.format())
        );
        JSONObject keyPayload = fetchJson(
                buildApiUrl(baseUrl + "/dapi/exam/api/key/"),
                buildHeaders(referer, cookieJar.format())
        );
        List<EzQuestion> rawQuestions = parseFormQuestions(formPayload);
        mergeAnswerKeys(rawQuestions, keyPayload);
        String examName = compactExamName(firstNonBlank(session.getName(), inferExamName(formPayload, sessionId)));
        List<EzQuestion> exportQuestions = dedupeQuestions(rawQuestions);
        ExportBundle bundle = new ExportBundle();
        bundle.setExamName(examName);
        bundle.setRawQuestions(rawQuestions);
        bundle.setExportQuestions(exportQuestions);
        bundle.setRawCount(rawQuestions.size());
        bundle.setExportedCount(exportQuestions.size());
        bundle.setDuplicateCount(rawQuestions.size() - exportQuestions.size());
        bundle.setOutputDir(outputDir);
        return bundle;
    }

    private List<EztestSessionVO> fetchSessionCandidates(String fixedExamId, String permit, CookieJar cookieJar) {
        String baseUrl = DEFAULT_BASE_URL;
        String referer = baseUrl + "/exam/" + fixedExamId + "/uniform/login/";
        String url = baseUrl + "/dapi/exam/" + fixedExamId + "/united_session_info/" + DEFAULT_LAN + "/"
                + "?permit=" + encode(permit)
                + "&is_client=false"
                + "&is_app=false"
                + "&practice_mode=true";
        JSONObject payload = fetchJsonWithCookies(url, buildHeaders(referer, cookieJar.format()), cookieJar);
        List<EztestSessionVO> sessions = extractSessionCandidates(payload);
        if (sessions.isEmpty()) {
            throw new BizException("题库列表接口未解析到可选题库");
        }
        return sessions;
    }

    private void loginSelectedSession(String permit,
                                      EztestSessionVO session,
                                      String referer,
                                      CookieJar cookieJar) {
        String baseUrl = DEFAULT_BASE_URL;
        String loginUrl = buildApiUrl(baseUrl + "/dapi/exam/api/login/");
        JSONObject body = JSONUtil.createObj()
                .set("permit", permit)
                .set("session", session.getSessionId());
        JSONObject result = postJsonWithCookies(loginUrl, buildHeaders(referer, cookieJar.format()), cookieJar, body);
        String failure = findLoginFailureMessage(result);
        if (hasText(failure)) {
            throw new BizException("登录接口返回失败: " + failure);
        }
        String entrySessionid = maybeGetEntrySessionid(result);
        if (hasText(entrySessionid)) {
            cookieJar.put("entry_sessionid", entrySessionid);
        } else if (hasText(session.getEntrySessionid())) {
            cookieJar.put("entry_sessionid", session.getEntrySessionid());
        }
        if (!hasText(cookieJar.get("entry_sessionid"))) {
            throw new BizException("登录后未拿到 entry_sessionid");
        }
    }

    private JSONObject fetchJson(String url, Headers headers) {
        Request request = new Request.Builder().url(url).headers(headers).get().build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BizException("EZTest 接口返回错误: " + response.code() + " - " + abbreviate(body, 300));
            }
            return parseJsonObject(body, url);
        } catch (IOException e) {
            throw new BizException("请求 EZTest 失败: " + e.getMessage());
        }
    }

    private JSONObject fetchJsonWithCookies(String url, Headers headers, CookieJar cookieJar) {
        Request request = new Request.Builder().url(url).headers(headers).get().build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            cookieJar.mergeSetCookie(response.headers("Set-Cookie"));
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BizException("EZTest 接口返回错误: " + response.code() + " - " + abbreviate(body, 300));
            }
            return parseJsonObject(body, url);
        } catch (IOException e) {
            throw new BizException("请求 EZTest 失败: " + e.getMessage());
        }
    }

    private JSONObject postJsonWithCookies(String url, Headers headers, CookieJar cookieJar, JSONObject body) {
        RequestBody requestBody = RequestBody.create(
                body.toString().getBytes(StandardCharsets.UTF_8),
                MediaType.parse("application/json")
        );
        Request request = new Request.Builder().url(url).headers(headers).post(requestBody).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            cookieJar.mergeSetCookie(response.headers("Set-Cookie"));
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new BizException("EZTest 登录接口返回错误: " + response.code() + " - " + abbreviate(responseBody, 300));
            }
            return parseJsonObject(responseBody, url);
        } catch (IOException e) {
            throw new BizException("请求 EZTest 登录失败: " + e.getMessage());
        }
    }

    private JSONObject parseJsonObject(String body, String url) {
        try {
            Object parsed = JSONUtil.parse(body);
            if (parsed instanceof JSONObject object) {
                return object;
            }
            throw new BizException("接口返回不是 JSON 对象: " + url);
        } catch (Exception e) {
            throw new BizException("接口返回不是合法 JSON: " + url + "，响应片段：" + abbreviate(body, 300));
        }
    }

    private Headers buildHeaders(String referer, String cookie) {
        Headers.Builder builder = new Headers.Builder()
                .add("Accept", "application/json, text/plain, */*")
                .add("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .add("Cache-Control", "no-cache, no-store, must-revalidate")
                .add("Content-Type", "application/json; charset=UTF-8")
                .add("Expires", "0")
                .add("Pragma", "no-cache")
                .add("Referer", referer)
                .add("User-Agent", DEFAULT_USER_AGENT);
        if (hasText(cookie)) {
            builder.add("Cookie", cookie);
        }
        return builder.build();
    }

    private List<EztestSessionVO> extractSessionCandidates(Object payload) {
        List<EztestSessionVO> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        visitSessionNode(payload, new ArrayList<>(), result, seen);
        return result;
    }

    private void visitSessionNode(Object node, List<String> path, List<EztestSessionVO> result, Set<String> seen) {
        if (node instanceof JSONObject object) {
            if (isProbableSessionItem(object, path)) {
                String sessionId = maybeGetSessionId(object);
                if (hasText(sessionId) && seen.add(sessionId)) {
                    EztestSessionVO vo = new EztestSessionVO();
                    vo.setSessionId(sessionId);
                    vo.setName(simplifySessionName(pickFirstField(object, SESSION_NAME_FIELDS)));
                    vo.setStatus(pickFirstField(object, SESSION_STATUS_FIELDS));
                    vo.setStatusText(translateStatus(vo.getStatus()));
                    vo.setTime(buildSessionTimeText(object));
                    vo.setEntrySessionid(maybeGetEntrySessionid(object));
                    result.add(vo);
                }
            }
            for (String key : object.keySet()) {
                path.add(key);
                visitSessionNode(object.get(key), path, result, seen);
                path.remove(path.size() - 1);
            }
            return;
        }
        if (node instanceof JSONArray array) {
            for (int i = 0; i < array.size(); i++) {
                path.add(String.valueOf(i));
                visitSessionNode(array.get(i), path, result, seen);
                path.remove(path.size() - 1);
            }
        }
    }

    private boolean isProbableSessionItem(JSONObject object, List<String> path) {
        if (!hasText(maybeGetSessionId(object))) {
            return false;
        }
        String pathText = String.join(" ", path).toLowerCase(Locale.ROOT);
        return pathText.contains("session")
                || hasText(pickFirstField(object, SESSION_NAME_FIELDS))
                || hasText(pickFirstField(object, SESSION_STATUS_FIELDS))
                || hasText(pickFirstField(object, SESSION_TIME_FIELDS));
    }

    private List<EzQuestion> parseFormQuestions(JSONObject payload) {
        JSONObject root = getFormRoot(payload);
        JSONArray sections = root.getJSONArray("sections");
        List<EzQuestion> questions = new ArrayList<>();
        int index = 0;
        for (Object sectionNode : sections) {
            if (!(sectionNode instanceof JSONObject section)) {
                continue;
            }
            String sectionName = normalizeText(firstNonBlank(section.getStr("name"), section.getStr("title")));
            JSONArray groups = section.getJSONArray("groups");
            if (groups == null) {
                continue;
            }
            for (Object groupNode : groups) {
                if (!(groupNode instanceof JSONObject group)) {
                    continue;
                }
                String groupName = normalizeText(firstNonBlank(group.getStr("name"), group.getStr("title")));
                JSONArray items = group.getJSONArray("items");
                if (items == null) {
                    continue;
                }
                for (Object itemNode : items) {
                    if (!(itemNode instanceof JSONObject item)) {
                        continue;
                    }
                    JSONObject content = item.getJSONObject("content");
                    if (content == null) {
                        content = JSONUtil.createObj();
                    }
                    EzQuestion question = new EzQuestion();
                    question.setIndex(++index);
                    question.setQuestionId(normalizeText(item.get("id")));
                    question.setQuestionName(normalizeText(item.get("name")));
                    question.setSectionName(sectionName);
                    question.setGroupName(groupName);
                    question.setRawType(normalizeText(item.get("type")).toLowerCase(Locale.ROOT));
                    question.setType(mapQuestionType(question.getRawType()));
                    question.setScore(normalizeText(item.get("point")));
                    question.setStem(htmlToText(content.get("stem")));
                    question.setAnalysis(htmlToText(content.get("analysis")));
                    question.setCorrectAnswer("");
                    List<EzOption> options = new ArrayList<>();
                    JSONArray optionArray = content.getJSONArray("options");
                    if (optionArray != null) {
                        for (Object optionNode : optionArray) {
                            if (!(optionNode instanceof JSONObject optionObject)) {
                                continue;
                            }
                            String optionId = normalizeText(optionObject.get("id"));
                            if (!hasText(optionId)) {
                                continue;
                            }
                            EzOption option = new EzOption();
                            option.setId(optionId);
                            option.setText(pickOptionText(optionObject));
                            options.add(option);
                        }
                    }
                    question.setOptions(options);
                    questions.add(question);
                }
            }
        }
        if (questions.isEmpty()) {
            throw new BizException("form 接口响应中未解析到任何题目");
        }
        return questions;
    }

    private JSONObject getFormRoot(JSONObject payload) {
        if (payload.get("sections") instanceof JSONArray) {
            return payload;
        }
        JSONObject form = payload.getJSONObject("form");
        if (form != null && form.get("sections") instanceof JSONArray) {
            return form;
        }
        throw new BizException("form 接口响应中未找到 sections");
    }

    private void mergeAnswerKeys(List<EzQuestion> questions, JSONObject keyPayload) {
        JSONArray keyItems = getKeyItems(keyPayload);
        Map<String, JSONObject> byId = new HashMap<>();
        Map<String, JSONObject> byName = new HashMap<>();
        for (Object node : keyItems) {
            if (node instanceof JSONObject item) {
                String id = normalizeText(item.get("id"));
                String name = normalizeText(item.get("name"));
                if (hasText(id)) {
                    byId.put(id, item);
                }
                if (hasText(name)) {
                    byName.put(name, item);
                }
            }
        }
        for (EzQuestion question : questions) {
            JSONObject keyItem = byId.get(question.getQuestionId());
            if (keyItem == null) {
                keyItem = byName.get(question.getQuestionName());
            }
            if (keyItem == null) {
                continue;
            }
            List<String> optionIds = question.getOptions().stream().map(EzOption::getId).toList();
            question.setCorrectAnswer(String.join(",", parseAnswerTokens(keyItem.get("key"), optionIds)));
            String analysis = htmlToText(keyItem.get("analysis"));
            if (hasText(analysis)) {
                question.setAnalysis(analysis);
            }
            if (keyItem.get("score") != null) {
                question.setScore(normalizeText(keyItem.get("score")));
            }
        }
    }

    private JSONArray getKeyItems(JSONObject payload) {
        JSONObject key = payload.getJSONObject("key");
        if (key != null && key.get("items") instanceof JSONArray items) {
            return items;
        }
        if (payload.get("items") instanceof JSONArray items) {
            return items;
        }
        throw new BizException("key 接口响应中未找到 key.items");
    }

    private List<String> parseAnswerTokens(Object rawKey, List<String> optionIds) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        collectAnswerTokens(rawKey, optionIds, tokens);
        return new ArrayList<>(tokens);
    }

    private void collectAnswerTokens(Object rawKey, List<String> optionIds, LinkedHashSet<String> tokens) {
        if (rawKey == null) {
            return;
        }
        if (rawKey instanceof JSONArray array) {
            for (Object item : array) {
                collectAnswerTokens(item, optionIds, tokens);
            }
            return;
        }
        if (rawKey instanceof JSONObject object) {
            for (String key : object.keySet()) {
                Object flag = object.get(key);
                if (flag instanceof Number number && number.doubleValue() == 0D) {
                    continue;
                }
                if (flag instanceof Boolean bool && !bool) {
                    continue;
                }
                addSplitAnswerToken(key, optionIds, tokens);
            }
            return;
        }
        String text = normalizeText(rawKey);
        for (String piece : text.split("[\\s,，;/|]+")) {
            addSplitAnswerToken(piece, optionIds, tokens);
        }
    }

    private void addSplitAnswerToken(String token, List<String> optionIds, LinkedHashSet<String> tokens) {
        String normalized = normalizeText(token);
        if (!hasText(normalized)) {
            return;
        }
        List<String> split = splitCompoundAnswerToken(normalized, optionIds);
        if (split == null || split.isEmpty()) {
            tokens.add(normalized);
        } else {
            tokens.addAll(split);
        }
    }

    private List<String> splitCompoundAnswerToken(String token, List<String> optionIds) {
        String normalized = token.replaceAll("[\\s,，;/|]+", "").toUpperCase(Locale.ROOT);
        if (!hasText(normalized)) {
            return Collections.emptyList();
        }
        Map<String, String> optionMap = new HashMap<>();
        for (String optionId : optionIds) {
            if (hasText(optionId)) {
                optionMap.put(optionId.toUpperCase(Locale.ROOT), optionId);
            }
        }
        if (optionMap.containsKey(normalized)) {
            return List.of(optionMap.get(normalized));
        }
        List<String> sorted = optionMap.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        List<String> result = new ArrayList<>();
        int cursor = 0;
        while (cursor < normalized.length()) {
            String matched = null;
            for (String candidate : sorted) {
                if (normalized.startsWith(candidate, cursor)) {
                    matched = candidate;
                    break;
                }
            }
            if (matched == null) {
                return null;
            }
            result.add(optionMap.get(matched));
            cursor += matched.length();
        }
        return result;
    }

    private List<EzQuestion> dedupeQuestions(List<EzQuestion> rawQuestions) {
        Map<String, EzQuestion> byKey = new LinkedHashMap<>();
        for (EzQuestion question : rawQuestions) {
            String key = buildQuestionContentKey(question);
            EzQuestion existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, question.copy());
            } else {
                mergeQuestion(existing, question);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private void mergeQuestion(EzQuestion target, EzQuestion source) {
        if (!hasText(target.getCorrectAnswer()) && hasText(source.getCorrectAnswer())) {
            target.setCorrectAnswer(source.getCorrectAnswer());
        }
        if (!hasText(target.getAnalysis()) && hasText(source.getAnalysis())) {
            target.setAnalysis(source.getAnalysis());
        }
        if (!hasText(target.getScore()) && hasText(source.getScore())) {
            target.setScore(source.getScore());
        }
        if ((target.getOptions() == null || target.getOptions().isEmpty()) && source.getOptions() != null) {
            target.setOptions(source.getOptions().stream().map(EzOption::copy).toList());
        }
    }

    private Path writeXlsx(ExportBundle bundle, Path outputDir) throws IOException {
        Path path = uniqueOutputPath(outputDir, bundle.getExamName(), ".xlsx");
        List<String> optionIds = collectOptionIds(bundle.getExportQuestions());
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("题目模板");
            Row header = sheet.createRow(0);
            List<String> headers = new ArrayList<>(List.of("题目内容", "题型", "正确答案", "解析", "难度"));
            headers.addAll(optionIds.stream().map(id -> "选项" + id).toList());
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            int rowIndex = 1;
            for (EzQuestion question : bundle.getExportQuestions()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(cleanStemForOutput(question));
                row.createCell(1).setCellValue(nullToEmpty(question.getType()));
                row.createCell(2).setCellValue(nullToEmpty(question.getCorrectAnswer()));
                row.createCell(3).setCellValue(nullToEmpty(question.getAnalysis()));
                row.createCell(4).setCellValue("");
                Map<String, String> optionsById = question.getOptions().stream()
                        .collect(Collectors.toMap(EzOption::getId, EzOption::getText, (a, b) -> a, LinkedHashMap::new));
                for (int i = 0; i < optionIds.size(); i++) {
                    row.createCell(5 + i).setCellValue(optionsById.getOrDefault(optionIds.get(i), ""));
                }
            }
            for (int i = 0; i < Math.min(headers.size(), 12); i++) {
                sheet.autoSizeColumn(i);
            }
            Files.createDirectories(path.getParent());
            try (var output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }
        return path;
    }

    private Path writePdf(ExportBundle bundle, Path outputDir, boolean includeAnswers) throws IOException {
        String suffix = includeAnswers ? "_含答案" : "_不含答案";
        Path path = uniqueOutputPath(outputDir, bundle.getExamName() + suffix, ".pdf");
        Files.createDirectories(path.getParent());
        try (PDDocument document = new PDDocument()) {
            try (PdfFontResource fontResource = loadPdfFont(document)) {
                try (PdfWriter writer = new PdfWriter(document, fontResource.getFont())) {
                    writer.addTitle(bundle.getExamName() + (includeAnswers ? "（含答案）" : "（不含答案）"));
                    int index = 1;
                    for (EzQuestion question : bundle.getExportQuestions()) {
                        String stem = cleanStemForOutput(question);
                        if (includeAnswers) {
                            writer.writeAnswerStem(index + ". [" + nullToEmpty(question.getType()) + "] ", stem, question.getCorrectAnswer(), 12);
                        } else {
                            writer.writeParagraph(index + ". [" + nullToEmpty(question.getType()) + "] " + stem, 12, true);
                        }
                        Set<String> correctOptionIds = parseCorrectOptionIds(question.getCorrectAnswer());
                        for (EzOption option : sortedOptions(question.getOptions())) {
                            String optionLine = option.getId() + ". " + nullToEmpty(option.getText());
                            if (includeAnswers && correctOptionIds.contains(normalizeText(option.getId()).toUpperCase(Locale.ROOT))) {
                                writer.writeOptionParagraph(optionLine, 11, true);
                            } else {
                                writer.writeOptionParagraph(optionLine, 11, false);
                            }
                        }
                        if (includeAnswers && hasText(question.getAnalysis())) {
                            writer.writeParagraph("解析：" + question.getAnalysis(), 11, false);
                        }
                        writer.writeBlank(8);
                        index++;
                    }
                }
                document.save(path.toFile());
            }
        }
        return path;
    }

    private Set<String> parseCorrectOptionIds(String correctAnswer) {
        String text = normalizeText(correctAnswer).toUpperCase(Locale.ROOT);
        if (!hasText(text)) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("[A-Z0-9]+").matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            result.add(token);
            if (token.matches("[A-Z]{2,}")) {
                for (int i = 0; i < token.length(); i++) {
                    result.add(String.valueOf(token.charAt(i)));
                }
            }
        }
        return result;
    }

    private PdfFontResource loadPdfFont(PDDocument document) throws IOException {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of("C:/Windows/Fonts/msyh.ttc"));
        candidates.add(Path.of("C:/Windows/Fonts/simsun.ttc"));
        candidates.add(Path.of("C:/Windows/Fonts/simhei.ttf"));
        candidates.add(Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"));
        candidates.add(Path.of("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"));
        candidates.add(Path.of("/System/Library/Fonts/PingFang.ttc"));
        for (Path candidate : candidates) {
            try {
                if (candidate != null && Files.exists(candidate)) {
                    if (candidate.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ttc")) {
                        return loadFontFromCollection(document, candidate);
                    }
                    return new PdfFontResource(PDType0Font.load(document, candidate.toFile()), null);
                }
            } catch (Exception e) {
                log.warn("加载 PDF 字体失败: {}", candidate, e);
            }
        }
        throw new BizException("未找到可用中文字体，请在服务器安装中文字体");
    }

    private PdfFontResource loadFontFromCollection(PDDocument document, Path path) throws IOException {
        TrueTypeCollection collection = new TrueTypeCollection(path.toFile());
        try {
            List<PDType0Font> loaded = new ArrayList<>(1);
            collection.processAllFonts(font -> {
                if (loaded.isEmpty()) {
                    loaded.add(PDType0Font.load(document, font, true));
                } else {
                    font.close();
                }
            });
            if (loaded.isEmpty()) {
                throw new IOException("字体集合为空");
            }
            return new PdfFontResource(loaded.get(0), collection);
        } catch (IOException | RuntimeException e) {
            try {
                collection.close();
            } catch (IOException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }

    private void saveFile(Long jobId, String fileType, Path path) throws IOException {
        EztestJobFile file = new EztestJobFile();
        file.setJobId(jobId);
        file.setFileType(fileType);
        file.setFileName(path.getFileName().toString());
        file.setFilePath(path.toAbsolutePath().normalize().toString());
        file.setFileSize(Files.size(path));
        eztestJobFileMapper.insert(file);
    }

    private int calculateExportProgress(int completed, int total) {
        int safeTotal = Math.max(total, 1);
        return PROGRESS_START + (PROGRESS_EXPORT_END - PROGRESS_START) * Math.min(completed, safeTotal) / safeTotal;
    }

    private int calculateFileProgress(int completed, int total) {
        if (total <= 0) {
            return PROGRESS_FILES_END;
        }
        return PROGRESS_CONVERTED + (PROGRESS_FILES_END - PROGRESS_CONVERTED) * Math.min(completed, total) / total;
    }

    private Map<String, Object> toConvertedQuestion(EzQuestion question) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", cleanStemForOutput(question));
        data.put("type", question.getType());
        data.put("answer", question.getCorrectAnswer());
        data.put("analysis", question.getAnalysis());
        data.put("difficulty", "");
        data.put("options", sortedOptions(question.getOptions()).stream().map(EzOption::getText).toList());
        return data;
    }

    private List<ConvertedBundle> toConvertedBundles(List<ExportBundle> bundles) {
        if (bundles == null) {
            return Collections.emptyList();
        }
        return bundles.stream()
                .map(bundle -> new ConvertedBundle(
                        bundle.getExamName(),
                        bundle.getExportQuestions().stream().map(this::toConvertedQuestion).toList()
                ))
                .toList();
    }

    private String buildImportPayload(List<ConvertedBundle> bundles) {
        JSONArray array = JSONUtil.createArray();
        for (ConvertedBundle bundle : bundles) {
            array.add(JSONUtil.createObj()
                    .set("bankName", bundle.bankName())
                    .set("questions", bundle.questions()));
        }
        return array.toString();
    }

    private List<ConvertedBundle> parseImportPayload(String payload) {
        if (!hasText(payload)) {
            return Collections.emptyList();
        }
        try {
            Object parsed = JSONUtil.parse(payload);
            if (!(parsed instanceof JSONArray array)) {
                return Collections.emptyList();
            }
            List<ConvertedBundle> result = new ArrayList<>();
            for (Object item : array) {
                if (!(item instanceof JSONObject object)) {
                    continue;
                }
                String bankName = normalizeText(object.get("bankName"));
                JSONArray questionArray = object.getJSONArray("questions");
                if (!hasText(bankName) || questionArray == null || questionArray.isEmpty()) {
                    continue;
                }
                List<Map<String, Object>> questions = new ArrayList<>();
                for (Object questionNode : questionArray) {
                    if (questionNode instanceof JSONObject questionObject) {
                        questions.add(toPlainQuestionMap(questionObject));
                    }
                }
                if (!questions.isEmpty()) {
                    result.add(new ConvertedBundle(bankName, questions));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BizException("任务导入数据解析失败，请重新导出");
        }
    }

    private Map<String, Object> toPlainQuestionMap(JSONObject questionObject) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("content", questionObject.getStr("content"));
        question.put("type", questionObject.getStr("type"));
        question.put("answer", questionObject.getStr("answer"));
        question.put("analysis", questionObject.getStr("analysis"));
        question.put("difficulty", questionObject.getStr("difficulty"));
        JSONArray options = questionObject.getJSONArray("options");
        List<String> optionList = new ArrayList<>();
        if (options != null) {
            for (Object option : options) {
                optionList.add(normalizeText(option));
            }
        }
        question.put("options", optionList);
        return question;
    }

    private QuestionService.QuestionImportResult importConvertedBundles(Long importBankId,
                                                                        Long importCategoryId,
                                                                        List<ConvertedBundle> bundles) {
        int createCount = 0;
        int updateCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        for (ConvertedBundle bundle : bundles) {
            QuestionService.QuestionImportResult result = questionService.importFromConverted(
                    importBankId,
                    importBankId == null ? importCategoryId : null,
                    bundle.bankName(),
                    bundle.questions()
            );
            createCount += result.getCreateCount();
            updateCount += result.getUpdateCount();
            failCount += result.getFailCount();
            if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                errors.addAll(result.getErrors().stream()
                        .map(error -> bundle.bankName() + "：" + error)
                        .toList());
            }
        }
        return new QuestionService.QuestionImportResult(createCount, updateCount, failCount, errors);
    }

    private boolean wantsImport(EztestExportDTO dto) {
        return dto != null && (dto.getImportBankId() != null || dto.getImportCategoryId() != null);
    }

    private EztestJob requireJob(Long id) {
        if (id == null) {
            throw new BizException("任务ID不能为空");
        }
        EztestJob job = eztestJobMapper.selectOneById(id);
        if (job == null) {
            throw new BizException("任务不存在");
        }
        return job;
    }

    private EztestJobVO toJobVO(EztestJob job, List<EztestJobFile> files) {
        EztestJobVO vo = BeanUtil.copyProperties(job, EztestJobVO.class);
        vo.setSessionIds(splitCsv(job.getSessionIds()));
        vo.setSessionNames(splitCsv(job.getSessionNames()));
        vo.setImportable(hasText(job.getImportPayload()) && !hasImportResult(job) ? 1 : 0);
        vo.setStatusText(jobStatusText(job.getStatus()));
        vo.setProgressPercent(nullToZero(job.getProgressPercent()));
        vo.setOperatorName(resolveOperatorName(job.getOperatorId()));
        vo.setFiles(files.stream().map(this::toFileVO).toList());
        return vo;
    }

    private void savePermitProfile(Long operatorId, String permit) {
        if (operatorId == null || !hasText(permit)) {
            return;
        }
        int updated = eztestProfileMapper.updatePermit(operatorId, permit.trim());
        if (updated > 0) {
            return;
        }
        EztestProfile profile = new EztestProfile();
        profile.setOperatorId(operatorId);
        profile.setPermit(permit.trim());
        try {
            eztestProfileMapper.insert(profile);
        } catch (Exception e) {
            eztestProfileMapper.updatePermit(operatorId, permit.trim());
        }
    }

    private boolean hasImportResult(EztestJob job) {
        if (job == null) {
            return false;
        }
        int resultCount = nullToZero(job.getImportCreateCount()) + nullToZero(job.getImportUpdateCount()) + nullToZero(job.getImportFailCount());
        if (resultCount > 0) {
            return true;
        }
        return (Objects.equals(job.getStatus(), JOB_SUCCESS) || Objects.equals(job.getStatus(), JOB_PARTIAL_FAILED))
                && (job.getImportBankId() != null || job.getImportCategoryId() != null);
    }

    private EztestJobFileVO toFileVO(EztestJobFile file) {
        EztestJobFileVO vo = BeanUtil.copyProperties(file, EztestJobFileVO.class);
        vo.setFileTypeText(fileTypeText(file.getFileType()));
        return vo;
    }

    private String resolveOperatorName(Long operatorId) {
        if (operatorId == null) {
            return "";
        }
        Admin admin = adminMapper.selectOneById(operatorId);
        if (admin == null) {
            return "";
        }
        return firstNonBlank(admin.getNickname(), admin.getUsername());
    }

    private String buildApiUrl(String base) {
        return base + "?lan=" + encode(DEFAULT_LAN) + "&_=" + System.currentTimeMillis();
    }

    private Path resolveJobOutputDir(Long jobId) throws IOException {
        Path path = resolveEztestOutputRoot().resolve(String.valueOf(jobId)).normalize();
        Files.createDirectories(path);
        return path;
    }

    private void deleteJobOutputDir(Long jobId) {
        if (jobId == null) {
            return;
        }
        Path dir = resolveEztestOutputRoot().resolve(String.valueOf(jobId)).normalize();
        Path root = resolveEztestOutputRoot();
        if (!dir.startsWith(root) || !Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("删除 EZTest 任务目录失败: {}", path, e);
                }
            });
        } catch (IOException e) {
            log.warn("扫描 EZTest 任务目录失败: {}", dir, e);
        }
    }

    private Path resolveEztestOutputRoot() {
        return Path.of(uploadPath).toAbsolutePath().normalize().resolve("eztest").normalize();
    }

    private Path resolveStoredFilePath(String filePath) {
        if (!hasText(filePath)) {
            throw new BizException("文件路径为空");
        }
        Path root = resolveEztestOutputRoot();
        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new BizException("文件路径非法");
        }
        return path;
    }

    private Path uniqueOutputPath(Path outputDir, String name, String ext) {
        String safeName = sanitizeFilename(name, "eztest_questions");
        Path path = outputDir.resolve(safeName + ext).normalize();
        int index = 2;
        while (Files.exists(path)) {
            path = outputDir.resolve(safeName + "_" + index + ext).normalize();
            index++;
        }
        return path;
    }

    private List<String> collectOptionIds(List<EzQuestion> questions) {
        return questions.stream()
                .flatMap(question -> question.getOptions().stream())
                .map(EzOption::getId)
                .filter(this::hasText)
                .distinct()
                .sorted(this::compareOptionIds)
                .toList();
    }

    private List<EzOption> sortedOptions(List<EzOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream()
                .sorted((a, b) -> compareOptionIds(a.getId(), b.getId()))
                .toList();
    }

    private int compareOptionIds(String a, String b) {
        return optionSortKey(a).compareTo(optionSortKey(b));
    }

    private String optionSortKey(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (normalized.matches("[A-Z]+")) {
            int result = 0;
            for (char c : normalized.toCharArray()) {
                result = result * 26 + (c - 'A' + 1);
            }
            return String.format("0-%08d-%s", result, normalized);
        }
        return "1-" + normalized;
    }

    private String cleanStemForOutput(EzQuestion question) {
        if (question == null) {
            return "";
        }
        return stripTrailingAnswerMarker(question.getStem(), question.getCorrectAnswer());
    }

    private String stripTrailingAnswerMarker(String stem, String correctAnswer) {
        String text = stem == null ? "" : stem.trim();
        if (!hasText(text)) {
            return "";
        }
        String normalizedAnswer = normalizeAnswerMarker(correctAnswer);
        Matcher matcher = TRAILING_ANSWER_PATTERN.matcher(text);
        if (matcher.find()) {
            String marker = normalizeAnswerMarker(matcher.group(1));
            boolean sameAsAnswer = hasText(normalizedAnswer) && marker.equals(normalizedAnswer);
            boolean fallbackChoiceMarker = !hasText(normalizedAnswer) && marker.matches("[A-Z]{1,6}");
            if (hasText(marker) && (sameAsAnswer || fallbackChoiceMarker)) {
                return text.substring(0, matcher.start()).trim();
            }
        }
        return text;
    }

    private String normalizeAnswerMarker(String value) {
        return normalizeText(value)
                .replaceAll("[\\s,，、/|]+", "")
                .toUpperCase(Locale.ROOT);
    }

    private String buildQuestionContentKey(EzQuestion question) {
        String optionText = sortedOptions(question.getOptions()).stream()
                .map(option -> normalizeText(option.getId()).toUpperCase(Locale.ROOT) + ":" + normalizeText(option.getText()))
                .collect(Collectors.joining("|"));
        return "[" + normalizeText(question.getType()) + "]" + normalizeText(cleanStemForOutput(question)) + "||" + optionText;
    }

    private String inferExamName(JSONObject formPayload, String sessionId) {
        JSONObject root = getFormRoot(formPayload);
        for (String key : List.of("title", "name", "exam_name", "paper_name", "paper_title", "subject_name")) {
            String value = normalizeText(root.get(key));
            if (hasText(value) && !value.matches("[A-Z]\\d{6,}")) {
                return value;
            }
        }
        return "eztest_session_" + sessionId;
    }

    private String pickOptionText(JSONObject option) {
        for (String key : List.of("description", "content", "text", "label")) {
            Object value = option.get(key);
            if (value != null && hasText(String.valueOf(value))) {
                return htmlToText(value);
            }
        }
        return "";
    }

    private String mapQuestionType(String rawType) {
        String normalized = normalizeText(rawType).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "sc" -> "单选题";
            case "mc" -> "多选题";
            case "tf" -> "判断题";
            default -> hasText(normalized) ? normalized.toUpperCase(Locale.ROOT) : "未知";
        };
    }

    private String htmlToText(Object value) {
        String text = String.valueOf(value == null ? "" : value);
        text = HtmlUtils.htmlUnescape(text);
        text = BR_PATTERN.matcher(text).replaceAll("\n");
        text = P_END_PATTERN.matcher(text).replaceAll("\n");
        text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
        text = text.replace('\u00A0', ' ');
        return Arrays.stream(text.split("\\R"))
                .map(this::normalizeText)
                .filter(this::hasText)
                .collect(Collectors.joining("\n"));
    }

    private String maybeGetSessionId(JSONObject object) {
        for (String key : SESSION_ID_FIELDS) {
            String value = normalizeText(object.get(key));
            if (value.matches("\\d+")) {
                return value;
            }
        }
        return "";
    }

    private String maybeGetEntrySessionid(Object node) {
        if (node instanceof JSONObject object) {
            for (String key : ENTRY_SESSIONID_FIELDS) {
                String value = normalizeText(object.get(key));
                String parsed = parseEntrySessionid(value);
                if (hasText(parsed)) {
                    return parsed;
                }
            }
            for (String key : object.keySet()) {
                String parsed = maybeGetEntrySessionid(object.get(key));
                if (hasText(parsed)) {
                    return parsed;
                }
            }
        } else if (node instanceof JSONArray array) {
            for (Object item : array) {
                String parsed = maybeGetEntrySessionid(item);
                if (hasText(parsed)) {
                    return parsed;
                }
            }
        } else if (node != null) {
            return parseEntrySessionid(String.valueOf(node));
        }
        return "";
    }

    private String parseEntrySessionid(String value) {
        String text = normalizeText(value);
        if (!hasText(text)) {
            return "";
        }
        Matcher matcher = Pattern.compile("entry_sessionid=([^;]+)").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return text.contains(":ENTRY:") ? text : "";
    }

    private String findLoginFailureMessage(Object payload) {
        if (payload instanceof JSONObject object) {
            for (String key : object.keySet()) {
                Object value = object.get(key);
                String lowerKey = key.toLowerCase(Locale.ROOT);
                if (("success".equals(lowerKey) || "ok".equals(lowerKey)) && Boolean.FALSE.equals(value)) {
                    return key + "=False";
                }
                if (List.of("status", "state", "result", "message", "msg", "error", "detail", "reason").contains(lowerKey)
                        && containsFailureToken(value)) {
                    return normalizeText(value);
                }
            }
            for (String key : object.keySet()) {
                String result = findLoginFailureMessage(object.get(key));
                if (hasText(result)) {
                    return result;
                }
            }
        } else if (payload instanceof JSONArray array) {
            for (Object item : array) {
                String result = findLoginFailureMessage(item);
                if (hasText(result)) {
                    return result;
                }
            }
        }
        return "";
    }

    private boolean containsFailureToken(Object value) {
        String text = normalizeText(value).toLowerCase(Locale.ROOT);
        if (!hasText(text)) {
            return false;
        }
        return List.of("error", "fail", "failed", "failure", "invalid", "denied", "forbidden", "incorrect", "not found",
                "失败", "错误", "无效", "拒绝", "禁止", "不存在", "未找到", "不正确").stream().anyMatch(text::contains);
    }

    private String pickFirstField(JSONObject object, List<String> fields) {
        for (String field : fields) {
            String value = normalizeText(object.get(field));
            if (hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String buildSessionTimeText(JSONObject object) {
        String direct = pickFirstField(object, SESSION_TIME_FIELDS);
        String start = pickFirstField(object, SESSION_START_FIELDS);
        String end = pickFirstField(object, SESSION_END_FIELDS);
        if (hasText(start) && hasText(end)) {
            return start + " - " + end;
        }
        return firstNonBlank(direct, start, end);
    }

    private String translateStatus(String status) {
        String normalized = normalizeText(status).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ongoing" -> "考试中";
            case "upcoming", "not_started", "pending" -> "未开始";
            case "finished", "ended", "closed" -> "已结束";
            default -> hasText(status) ? status : "未知";
        };
    }

    private String simplifySessionName(String name) {
        return compactExamName(normalizeText(name));
    }

    private String compactExamName(String name) {
        String text = normalizeText(name);
        String compacted = TRAILING_SESSION_TIME_PATTERN.matcher(text).replaceAll("").replaceAll("[\\s\\-_]+$", "");
        return hasText(compacted) ? compacted : text;
    }

    private String sanitizeFilename(String value, String fallback) {
        String sanitized = normalizeText(value).replaceAll("[<>:\"/\\\\|?*]+", "_").replaceAll("^[ .]+|[ .]+$", "");
        return hasText(sanitized) ? sanitized : fallback;
    }

    private String safeSessionLabel(EztestSessionVO session) {
        return firstNonBlank(session.getName(), "session " + session.getSessionId());
    }

    private EztestExportDTO cloneExportDto(EztestExportDTO dto) {
        EztestExportDTO copy = new EztestExportDTO();
        copy.setPermit(dto.getPermit());
        copy.setSessionIds(dto.getSessionIds() == null ? Collections.emptyList() : new ArrayList<>(dto.getSessionIds()));
        copy.setExportXlsx(dto.getExportXlsx());
        copy.setExportPdfWithAnswers(dto.getExportPdfWithAnswers());
        copy.setExportPdfWithoutAnswers(dto.getExportPdfWithoutAnswers());
        copy.setImportBankId(dto.getImportBankId());
        copy.setImportCategoryId(dto.getImportCategoryId());
        return copy;
    }

    private String joinDistinct(List<String> values) {
        return values == null ? "" : values.stream()
                .map(this::normalizeText)
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private List<String> splitCsv(String value) {
        if (!hasText(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(this::normalizeText)
                .filter(this::hasText)
                .toList();
    }

    private String appendLog(String logs, String message) {
        String line = "[" + LocalDateTime.now().withNano(0) + "] " + message;
        if (!hasText(logs)) {
            return line;
        }
        return logs + "\n" + line;
    }

    private String maskSensitive(String value) {
        String text = normalizeText(value);
        if (text.length() <= 4) {
            return "*".repeat(text.length());
        }
        return "*".repeat(text.length() - 4) + text.substring(text.length() - 4);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeText(Object value) {
        return value == null ? "" : String.valueOf(value).replaceAll("\\s+", " ").trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private String jobStatusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case JOB_PENDING -> "排队中";
            case JOB_RUNNING -> "执行中";
            case JOB_SUCCESS -> "已完成";
            case JOB_PARTIAL_FAILED -> "完成但有失败";
            case JOB_FAILED -> "执行异常";
            default -> "未知";
        };
    }

    private String fileTypeText(String fileType) {
        return switch (fileType == null ? "" : fileType) {
            case FILE_TYPE_XLSX -> "XLSX";
            case FILE_TYPE_PDF_WITH_ANSWERS -> "PDF含答案";
            case FILE_TYPE_PDF_WITHOUT_ANSWERS -> "PDF不含答案";
            default -> fileType;
        };
    }

    private String resolveContentType(String fileType) {
        if (FILE_TYPE_XLSX.equals(fileType)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (FILE_TYPE_PDF_WITH_ANSWERS.equals(fileType) || FILE_TYPE_PDF_WITHOUT_ANSWERS.equals(fileType)) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    private static class CookieJar {
        private final Map<String, String> cookies = new LinkedHashMap<>();

        CookieJar(String cookieHeader) {
            if (cookieHeader == null) {
                return;
            }
            for (String segment : cookieHeader.split(";")) {
                String part = segment.trim();
                int index = part.indexOf('=');
                if (index <= 0) {
                    continue;
                }
                String key = part.substring(0, index).trim();
                String value = part.substring(index + 1).trim();
                if (!key.isEmpty()) {
                    cookies.put(key, value);
                }
            }
        }

        void mergeSetCookie(List<String> setCookieHeaders) {
            if (setCookieHeaders == null) {
                return;
            }
            for (String header : setCookieHeaders) {
                if (header == null) {
                    continue;
                }
                String first = header.split(";", 2)[0].trim();
                int index = first.indexOf('=');
                if (index <= 0) {
                    continue;
                }
                cookies.put(first.substring(0, index).trim(), first.substring(index + 1).trim());
            }
        }

        void put(String key, String value) {
            if (key != null && value != null) {
                cookies.put(key, value);
            }
        }

        String get(String key) {
            return cookies.get(key);
        }

        String format() {
            return cookies.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("; "));
        }
    }

    @Data
    private static class EzOption {
        private String id;
        private String text;

        EzOption copy() {
            EzOption option = new EzOption();
            option.setId(id);
            option.setText(text);
            return option;
        }
    }

    @Data
    private static class EzQuestion {
        private int index;
        private String questionId;
        private String questionName;
        private String sectionName;
        private String groupName;
        private String rawType;
        private String type;
        private String score;
        private String stem;
        private List<EzOption> options = new ArrayList<>();
        private String correctAnswer;
        private String analysis;

        EzQuestion copy() {
            EzQuestion question = new EzQuestion();
            question.setIndex(index);
            question.setQuestionId(questionId);
            question.setQuestionName(questionName);
            question.setSectionName(sectionName);
            question.setGroupName(groupName);
            question.setRawType(rawType);
            question.setType(type);
            question.setScore(score);
            question.setStem(stem);
            question.setOptions(options == null ? new ArrayList<>() : options.stream().map(EzOption::copy).collect(Collectors.toCollection(ArrayList::new)));
            question.setCorrectAnswer(correctAnswer);
            question.setAnalysis(analysis);
            return question;
        }
    }

    @Data
    private static class ExportBundle {
        private String examName;
        private List<EzQuestion> rawQuestions;
        private List<EzQuestion> exportQuestions;
        private int rawCount;
        private int exportedCount;
        private int duplicateCount;
        private Path outputDir;
    }

    private record ConvertedBundle(String bankName, List<Map<String, Object>> questions) {
    }

    private static class PdfFontResource implements AutoCloseable {
        private final PDType0Font font;
        private final TrueTypeCollection collection;

        PdfFontResource(PDType0Font font, TrueTypeCollection collection) {
            this.font = font;
            this.collection = collection;
        }

        PDType0Font getFont() {
            return font;
        }

        @Override
        public void close() throws IOException {
            if (collection != null) {
                collection.close();
            }
        }
    }

    private static class PdfWriter implements AutoCloseable {
        private final PDDocument document;
        private final PDType0Font font;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;
        private final float margin = 48F;
        private final float width = PDRectangle.A4.getWidth() - margin * 2;
        private final float optionIndent = 18F;

        PdfWriter(PDDocument document, PDType0Font font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        void addTitle(String title) throws IOException {
            writeParagraph(title, 16, true);
            writeBlank(12);
        }

        void writeParagraph(String text, int fontSize, boolean bold) throws IOException {
            writeParagraph(text, fontSize, bold, PdfColor.BLACK);
        }

        void writeOptionParagraph(String text, int fontSize, boolean correct) throws IOException {
            PdfColor color = correct ? PdfColor.RED : PdfColor.BLACK;
            List<String> lines = wrapText(text == null ? "" : text, fontSize, width - optionIndent);
            for (String line : lines) {
                writeLine(List.of(new TextRun(line, color)), fontSize, optionIndent);
            }
        }

        void writeParagraph(String text, int fontSize, boolean bold, PdfColor color) throws IOException {
            List<String> lines = wrapText(text == null ? "" : text, fontSize);
            for (String line : lines) {
                writeLine(List.of(new TextRun(line, color)), fontSize);
            }
            if (bold) {
                writeBlank(2);
            }
        }

        void writeAnswerStem(String prefix, String stem, String correctAnswer, int fontSize) throws IOException {
            String answer = normalizeStatic(correctAnswer);
            List<TextRun> runs = buildAnswerRuns(prefix, stem, answer);
            List<List<TextRun>> lines = wrapRuns(runs, fontSize);
            for (List<TextRun> line : lines) {
                writeLine(line, fontSize);
            }
            writeBlank(2);
        }

        void writeBlank(float height) throws IOException {
            ensureSpace(height);
            y -= height;
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - margin;
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < margin) {
                newPage();
            }
        }

        private void writeLine(List<TextRun> runs, int fontSize) throws IOException {
            writeLine(runs, fontSize, 0F);
        }

        private void writeLine(List<TextRun> runs, int fontSize, float xOffset) throws IOException {
            ensureSpace(fontSize + 5);
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(margin + xOffset, y);
            PdfColor currentColor = null;
            for (TextRun run : runs) {
                if (run == null || run.text() == null || run.text().isEmpty()) {
                    continue;
                }
                PdfColor color = run.color() == null ? PdfColor.BLACK : run.color();
                if (currentColor != color) {
                    stream.setNonStrokingColor(color.awtColor());
                    currentColor = color;
                }
                stream.showText(run.text());
            }
            stream.endText();
            y -= fontSize + 5;
        }

        private List<String> wrapText(String text, int fontSize) throws IOException {
            return wrapText(text, fontSize, width);
        }

        private List<String> wrapText(String text, int fontSize, float maxWidth) throws IOException {
            List<String> result = new ArrayList<>();
            for (String paragraph : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
                String remaining = paragraph.trim();
                if (remaining.isEmpty()) {
                    result.add("");
                    continue;
                }
                StringBuilder line = new StringBuilder();
                for (int offset = 0; offset < remaining.length(); ) {
                    int codePoint = remaining.codePointAt(offset);
                    String ch = new String(Character.toChars(codePoint));
                    String candidate = line + ch;
                    float candidateWidth = font.getStringWidth(candidate) / 1000F * fontSize;
                    if (candidateWidth > maxWidth && line.length() > 0) {
                        result.add(line.toString());
                        line.setLength(0);
                        line.append(ch);
                    } else {
                        line.append(ch);
                    }
                    offset += Character.charCount(codePoint);
                }
                if (line.length() > 0) {
                    result.add(line.toString());
                }
            }
            return result;
        }

        private List<List<TextRun>> wrapRuns(List<TextRun> runs, int fontSize) throws IOException {
            List<List<TextRun>> result = new ArrayList<>();
            List<TextRun> line = new ArrayList<>();
            StringBuilder lineText = new StringBuilder();
            for (TextRun run : runs) {
                if (run == null || run.text() == null) {
                    continue;
                }
                PdfColor color = run.color() == null ? PdfColor.BLACK : run.color();
                for (int offset = 0; offset < run.text().length(); ) {
                    int codePoint = run.text().codePointAt(offset);
                    String ch = new String(Character.toChars(codePoint));
                    String candidate = lineText + ch;
                    float candidateWidth = font.getStringWidth(candidate) / 1000F * fontSize;
                    if (candidateWidth > width && lineText.length() > 0) {
                        result.add(line);
                        line = new ArrayList<>();
                        lineText.setLength(0);
                    }
                    appendRun(line, ch, color);
                    lineText.append(ch);
                    offset += Character.charCount(codePoint);
                }
            }
            if (!line.isEmpty()) {
                result.add(line);
            }
            return result;
        }

        private void appendRun(List<TextRun> line, String text, PdfColor color) {
            if (text == null || text.isEmpty()) {
                return;
            }
            if (!line.isEmpty()) {
                TextRun last = line.get(line.size() - 1);
                if (last.color() == color) {
                    line.set(line.size() - 1, new TextRun(last.text() + text, color));
                    return;
                }
            }
            line.add(new TextRun(text, color));
        }

        private List<TextRun> buildAnswerRuns(String prefix, String stem, String answer) {
            String cleanPrefix = normalizeStatic(prefix);
            String cleanStem = normalizeStatic(stem);
            if (answer.isEmpty()) {
                return List.of(new TextRun(cleanPrefix + cleanStem, PdfColor.BLACK));
            }
            Matcher matcher = ANSWER_BLANK_PATTERN.matcher(cleanStem);
            if (matcher.find()) {
                return List.of(
                        new TextRun(cleanPrefix + cleanStem.substring(0, matcher.start()) + "（", PdfColor.BLACK),
                        new TextRun(answer, PdfColor.RED),
                        new TextRun("）" + cleanStem.substring(matcher.end()), PdfColor.BLACK)
                );
            }
            return List.of(
                    new TextRun(cleanPrefix + cleanStem + "（", PdfColor.BLACK),
                    new TextRun(answer, PdfColor.RED),
                    new TextRun("）", PdfColor.BLACK)
            );
        }

        private static String normalizeStatic(String value) {
            return value == null ? "" : value.replaceAll("\\s+", " ").trim();
        }

        private record TextRun(String text, PdfColor color) {
        }

        private enum PdfColor {
            BLACK(new Color(0, 0, 0)),
            RED(new Color(204, 0, 0));

            private final Color awtColor;

            PdfColor(Color awtColor) {
                this.awtColor = awtColor;
            }

            Color awtColor() {
                return awtColor;
            }
        }
    }
}
