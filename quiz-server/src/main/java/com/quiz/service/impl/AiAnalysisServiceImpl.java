package com.quiz.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.config.AiBatchProperties;
import com.quiz.dto.admin.AiBatchGenerateDTO;
import com.quiz.dto.admin.AiConfigDTO;
import com.quiz.dto.admin.AiLogQueryDTO;
import com.quiz.entity.Admin;
import com.quiz.entity.AiBatchJob;
import com.quiz.entity.AiBatchJobItem;
import com.quiz.entity.AiCallLog;
import com.quiz.entity.AiConfig;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionOption;
import com.quiz.entity.User;
import com.quiz.mapper.AiBatchJobItemMapper;
import com.quiz.mapper.AiBatchJobMapper;
import com.quiz.mapper.AdminMapper;
import com.quiz.mapper.AiCallLogMapper;
import com.quiz.mapper.AiConfigMapper;
import com.quiz.mapper.QuestionMapper;
import com.quiz.mapper.QuestionOptionMapper;
import com.quiz.mapper.UserMapper;
import com.quiz.service.AiAnalysisService;
import com.quiz.util.AiProviderUtil;
import com.quiz.vo.admin.AiBatchJobItemVO;
import com.quiz.vo.admin.AiBatchJobVO;
import com.quiz.vo.admin.AiCallLogVO;
import com.quiz.vo.admin.AiStatsVO;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import okhttp3.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import java.util.stream.Collectors;

import static com.quiz.entity.table.AiCallLogTableDef.AI_CALL_LOG;
import static com.mybatisflex.core.query.QueryMethods.sum;
import static com.quiz.entity.table.QuestionTableDef.QUESTION;
import static com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService, ApplicationRunner {

    private static final int JOB_STATUS_PENDING = 0;
    private static final int JOB_STATUS_RUNNING = 1;
    private static final int JOB_STATUS_SUCCESS = 2;
    private static final int JOB_STATUS_PARTIAL_FAILED = 3;
    private static final int JOB_STATUS_FAILED = 4;
    private static final int JOB_STATUS_PAUSED = 5;
    private static final int JOB_STATUS_CANCELED = 6;
    private static final int ITEM_STATUS_PENDING = 0;
    private static final int ITEM_STATUS_RUNNING = 1;
    private static final int ITEM_STATUS_SUCCESS = 2;
    private static final int ITEM_STATUS_FAILED = 3;
    private static final int ITEM_STATUS_CANCELED = 4;

    private static final String DEFAULT_PROMPT_ANALYSIS = """
            {questionContext}

            请只输出纯文本解析，不要输出标题、Markdown、项目符号或空行。
            单选题、多选题：按每个选项分别说明正确或错误的原因，选项行使用“A. 选项内容：原因”的纯文本格式。
            判断题、填空题：只说明判断依据或填空原因。
            不要输出答案，不要以“解析：”“答案解析：”“题目解析：”开头。""";
    private static final String DEFAULT_PROMPT_ANSWER = """
            {questionContext}

            请直接输出答案。单选题输出一个选项字母，多选题用逗号分隔，判断题根据选项内容输出对应选项字母，不要假设 A 或 B 的含义，填空题输出应填内容。""";
    private static final String DEFAULT_PROMPT_BOTH = """
            {questionContext}

            请按以下纯文本格式输出，不要使用 Markdown 或空行：
            答案：[答案]
            解析：[单选题、多选题逐项说明每个选项原因；判断题、填空题只说明原因；判断题答案输出对应选项字母]
            解析内容不要再包含“解析：”“答案解析：”“题目解析：”等标题。""";

    private final AiConfigMapper aiConfigMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final AiBatchJobMapper aiBatchJobMapper;
    private final AiBatchJobItemMapper aiBatchJobItemMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final OkHttpClient okHttpClient;
    private final ThreadPoolTaskExecutor aiTaskExecutor;
    private final AiBatchProperties aiBatchProperties;
    private final Set<Long> activeBatchJobIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> resumeRequestedBatchJobIds = ConcurrentHashMap.newKeySet();

    @Override
    public void run(ApplicationArguments args) {
        recoverUnfinishedBatchJobs();
    }

    @Override
    public AiConfig getConfig() {
        List<AiConfig> list = aiConfigMapper.selectAll();
        if (list.isEmpty()) {
            return null;
        }
        AiConfig config = list.get(0);
        String provider = config.getProvider();
        if (provider == null || provider.isBlank()) {
            provider = AiProviderUtil.detectProvider(config.getBaseUrl());
            config.setProvider(provider);
        } else {
            provider = AiProviderUtil.normalizeProvider(provider);
            config.setProvider(provider);
        }
        config.setBaseUrl(AiProviderUtil.normalizeBaseUrl(provider, config.getBaseUrl()));
        return config;
    }

    @Override
    public void updateConfig(AiConfigDTO dto) {
        AiConfig config = getConfig();
        if (config == null) {
            config = new AiConfig();
        }
        String provider = AiProviderUtil.normalizeProvider(dto.getProvider());
        config.setProvider(provider);
        config.setBaseUrl(AiProviderUtil.normalizeBaseUrl(provider, dto.getBaseUrl()));
        config.setApiKey(dto.getApiKey() == null ? null : dto.getApiKey().trim());
        config.setModel(dto.getModel() == null ? null : dto.getModel().trim());
        config.setMaxTokens(dto.getMaxTokens());
        config.setTemperature(dto.getTemperature());
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        config.setUpdateTime(LocalDateTime.now());

        if (config.getId() != null) {
            aiConfigMapper.update(config);
        } else {
            config.setCreateTime(LocalDateTime.now());
            aiConfigMapper.insert(config);
        }
    }

    @Override
    public Map<String, Object> testConnection(AiConfigDTO dto) {
        AiConfig config = dto != null ? mergeWithStoredConfig(dto) : getConfig();
        if (config == null) throw new BizException("AI配置不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("provider", config.getProvider());
        result.put("apiKey", AiProviderUtil.maskApiKey(config.getApiKey()));
        result.put("baseUrl", config.getBaseUrl());
        result.put("model", config.getModel());

        try {
            AiTextResult aiResult = callAiWithRoute(config, "请回复：连接成功");
            result.put("success", true);
            result.put("reply", aiResult.content());
            result.put("mode", aiResult.route());
        } catch (Exception e) {
            log.error("AI连通测试失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listModels(AiConfigDTO dto) {
        AiConfig config = buildConfigForRequest(dto);
        String url = AiProviderUtil.buildModelsUrl(config.getProvider(), config.getBaseUrl());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Accept", "application/json")
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BizException("模型列表接口返回错误: " + response.code());
            }
            String responseBody = response.body().string();
            JSONObject json = JSONUtil.parseObj(responseBody);
            if (json.containsKey("error")) {
                JSONObject error = json.getJSONObject("error");
                String errorMsg = error != null ? error.getStr("message", "Unknown error") : "Unknown error";
                throw new BizException("模型列表接口错误: " + errorMsg);
            }

            cn.hutool.json.JSONArray data = json.getJSONArray("data");
            if (data == null) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> models = new ArrayList<>();
            for (Object item : data) {
                if (!(item instanceof JSONObject obj)) {
                    continue;
                }
                String id = obj.getStr("id");
                if (id == null || id.trim().isEmpty()) {
                    continue;
                }
                Map<String, Object> model = new HashMap<>();
                model.put("id", id);
                model.put("label", id);
                model.put("ownedBy", obj.getStr("owned_by"));
                models.add(model);
            }
            models.sort(Comparator.comparing(model -> String.valueOf(model.get("id"))));
            return models;
        } catch (IOException e) {
            throw new BizException("获取模型列表失败: " + e.getMessage());
        }
    }

    @Override
    public String generate(Long questionId, String mode, Long operatorId) {
        return generate(questionId, mode, operatorId, false);
    }

    private String generate(Long questionId, String mode, Long operatorId, boolean overwrite) {
        return generate(questionId, mode, operatorId, overwrite, () -> true);
    }

    private String generate(Long questionId, String mode, Long operatorId, boolean overwrite, BooleanSupplier canWriteResult) {
        AiConfig config = getConfig();
        if (config == null || config.getStatus() != 1) throw new BizException("AI服务未启用");

        Question question = questionMapper.selectOneById(questionId);
        if (question == null) throw new BizException("题目不存在");

        List<QuestionOption> options = questionOptionMapper.selectListByQuery(
                QueryWrapper.create().where(QUESTION_OPTION.QUESTION_ID.eq(questionId))
                        .orderBy(QUESTION_OPTION.SORT, true));
        String optionsStr = options.stream()
                .map(o -> o.getLabel() + ". " + o.getContent())
                .collect(Collectors.joining("\n"));

        String promptTemplate = resolvePromptTemplate(mode);
        if (promptTemplate == null || promptTemplate.trim().isEmpty()) {
            throw new BizException("当前模式未配置 Prompt 模板");
        }

        String questionContext = buildQuestionContext(question, optionsStr, mode, overwrite);
        String prompt = promptTemplate
                .replace("{questionContext}", questionContext)
                .replace("{content}", question.getContent())
                .replace("{type}", getQuestionTypeName(question.getType()))
                .replace("{options}", optionsStr)
                .replace("{answer}", shouldIncludeExistingAnswer(mode, overwrite) && question.getAnswer() != null ? question.getAnswer() : "")
                .replace("{analysis}", shouldIncludeExistingAnalysis(mode, overwrite) && question.getAnalysis() != null ? question.getAnalysis() : "");
        prompt = appendQuestionTypeInstruction(prompt, question, mode);

        long startTime = System.currentTimeMillis();
        AiCallLog callLog = new AiCallLog();
        callLog.setCallType("ADMIN");
        callLog.setQuestionId(questionId);
        callLog.setMode(mode);
        callLog.setPrompt(prompt);
        callLog.setOperatorId(operatorId);
        callLog.setCreateTime(LocalDateTime.now());

        try {
            AiTextResult aiResult = callAiWithRoute(config, prompt);
            String rawResult = aiResult.content();
            String result = rawResult;
            long costMs = System.currentTimeMillis() - startTime;
            callLog.setRoute(aiResult.route());
            callLog.setCostMs((int) costMs);
            callLog.setStatus(1);

            if (canWriteResult != null && !canWriteResult.getAsBoolean()) {
                throw new BizException("批量任务已取消，跳过写入");
            }

            // 更新题目
            if ("GENERATE_ANALYSIS".equals(mode)) {
                result = emptyIfNull(normalizeAnalysisText(rawResult));
                question.setAnalysis(result);
            } else if ("GENERATE_ANSWER".equals(mode)) {
                result = emptyIfNull(normalizeGeneratedAnswer(question, options, rawResult));
                question.setAnswer(result);
            } else if ("GENERATE_BOTH".equals(mode)) {
                Question parsedQuestion = new Question();
                parsedQuestion.setType(question.getType());
                parseBothResult(parsedQuestion, options, rawResult);
                if (!hasText(parsedQuestion.getAnswer()) || !hasText(parsedQuestion.getAnalysis())) {
                    throw new BizException("AI返回格式不符合要求，未解析到完整答案和解析");
                }
                question.setAnswer(parsedQuestion.getAnswer());
                question.setAnalysis(parsedQuestion.getAnalysis());
                result = buildBothResult(question);
            }
            callLog.setResult(result);
            questionMapper.update(question, false);

            aiCallLogMapper.insert(callLog);
            return result;
        } catch (Exception e) {
            callLog.setStatus(0);
            callLog.setErrorMsg(e.getMessage());
            callLog.setCostMs((int) (System.currentTimeMillis() - startTime));
            aiCallLogMapper.insert(callLog);
            throw new BizException("AI调用失败: " + e.getMessage());
        }
    }

    @Override
    public void generateAsync(Long questionId, String mode, Long operatorId) {
        aiTaskExecutor.execute(() -> {
            try {
                generate(questionId, mode, operatorId);
            } catch (Exception e) {
                log.error("异步AI生成失败, questionId={}, mode={}", questionId, mode, e);
            }
        });
    }

    @Override
    public Map<String, Object> batchGenerate(AiBatchGenerateDTO dto, Long operatorId) {
        if (dto == null) {
            throw new BizException("请求参数不能为空");
        }
        List<Question> questions;
        if (dto.getQuestionIds() != null && !dto.getQuestionIds().isEmpty()) {
            questions = questionMapper.selectListByIds(dto.getQuestionIds());
        } else if (dto.getBankId() != null) {
            questions = questionMapper.selectListByQuery(
                    QueryWrapper.create().where(QUESTION.BANK_ID.eq(dto.getBankId()))
                            .and(QUESTION.STATUS.eq(1)));
        } else {
            questions = questionMapper.selectListByQuery(
                    QueryWrapper.create().where(QUESTION.STATUS.eq(1)));
        }

        String mode = trimToNull(dto.getMode());
        resolvePromptTemplate(mode);
        boolean overwrite = Boolean.TRUE.equals(dto.getOverwrite());
        List<Question> eligibleQuestions = questions.stream()
                .filter(Objects::nonNull)
                .filter(question -> overwrite || !shouldSkipForMode(question, mode))
                .toList();

        int skippedCount = questions.size() - eligibleQuestions.size();

        List<Long> questionIds = eligibleQuestions.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (questionIds.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("total", questions.size());
            result.put("submitted", 0);
            result.put("skipped", skippedCount);
            result.put("jobId", null);
            result.put("message", skippedCount > 0
                    ? "没有符合条件的题目，已跳过" + skippedCount + "道已有内容的题目"
                    : "没有符合条件的题目");
            return result;
        }

        int concurrency = resolveBatchConcurrency(dto.getConcurrency());
        AiBatchJob job = createBatchJob(dto, mode, operatorId, questions.size(), questionIds.size(), skippedCount, concurrency, overwrite);
        List<AiBatchJobItem> items = createBatchJobItems(job.getId(), questionIds);
        aiTaskExecutor.execute(() -> runBatchGenerate(job.getId(), items, mode, operatorId, concurrency, overwrite));

        Map<String, Object> result = new HashMap<>();
        result.put("jobId", job.getId());
        result.put("total", questions.size());
        result.put("submitted", questionIds.size());
        result.put("skipped", skippedCount);
        result.put("concurrency", concurrency);
        result.put("message", "已提交" + questionIds.size() + "道题目的AI生成任务，并发数 " + concurrency + "，后台处理中"
                + (skippedCount > 0 ? "，并跳过" + skippedCount + "道已有内容的题目" : ""));
        return result;
    }

    @Override
    public AiBatchJobVO getBatchJob(Long jobId) {
        if (jobId == null) {
            throw new BizException("任务ID不能为空");
        }
        AiBatchJob job = aiBatchJobMapper.selectOneById(jobId);
        if (job == null) {
            throw new BizException("批量任务不存在");
        }
        return toBatchJobVO(job, true);
    }

    @Override
    public PageResult<AiBatchJobVO> getBatchJobList(Integer pageNum, Integer pageSize, Integer status, String mode) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (safePageNum - 1) * safePageSize;
        String safeMode = trimToNull(mode);
        List<AiBatchJob> jobs = aiBatchJobMapper.selectPage(offset, safePageSize, status, safeMode);
        List<AiBatchJobVO> voList = jobs.stream()
                .map(job -> toBatchJobVO(job, false))
                .collect(Collectors.toList());
        return PageResult.of(voList, aiBatchJobMapper.countAllJobs(status, safeMode), safePageNum, safePageSize);
    }

    @Override
    public void pauseBatchJob(Long jobId) {
        AiBatchJob job = requireBatchJob(jobId);
        if (!Objects.equals(job.getStatus(), JOB_STATUS_PENDING) && !Objects.equals(job.getStatus(), JOB_STATUS_RUNNING)) {
            throw new BizException("只有排队中或执行中的任务可以暂停");
        }
        aiBatchJobMapper.pauseJob(jobId, "手动暂停");
    }

    @Override
    public void resumeBatchJob(Long jobId) {
        AiBatchJob job = requireBatchJob(jobId);
        if (!Objects.equals(job.getStatus(), JOB_STATUS_PAUSED)) {
            throw new BizException("只有已暂停的任务可以继续");
        }
        if (activeBatchJobIds.contains(jobId)) {
            aiBatchJobMapper.resumeActiveJob(jobId);
            resumeRequestedBatchJobIds.add(jobId);
            if (!activeBatchJobIds.contains(jobId)) {
                restartResumedBatchJobIfNeeded(jobId, job.getMode(), job.getOperatorId(),
                        resolveBatchConcurrency(job.getConcurrency()), Objects.equals(job.getOverwrite(), 1));
            }
            return;
        }
        aiBatchJobItemMapper.resetRunningItems(jobId);
        aiBatchJobMapper.refreshCounts(jobId);
        List<AiBatchJobItem> items = aiBatchJobItemMapper.selectRecoverableItems(jobId);
        if (items.isEmpty()) {
            AiBatchJob refreshed = aiBatchJobMapper.selectOneById(jobId);
            int failed = defaultZero(refreshed != null ? refreshed.getFailCount() : job.getFailCount());
            aiBatchJobMapper.markFinished(jobId, failed > 0 ? JOB_STATUS_PARTIAL_FAILED : JOB_STATUS_SUCCESS, null, LocalDateTime.now());
            return;
        }
        aiBatchJobMapper.resumeJob(jobId);
        aiTaskExecutor.execute(() -> runBatchGenerate(jobId, items, job.getMode(), job.getOperatorId(),
                resolveBatchConcurrency(job.getConcurrency()), Objects.equals(job.getOverwrite(), 1)));
    }

    @Override
    public void retryFailedBatchJob(Long jobId) {
        AiBatchJob job = requireBatchJob(jobId);
        if (!Objects.equals(job.getStatus(), JOB_STATUS_PARTIAL_FAILED)
                && !Objects.equals(job.getStatus(), JOB_STATUS_FAILED)) {
            throw new BizException("只有完成但有失败或执行异常的任务可以重试失败题目");
        }
        if (activeBatchJobIds.contains(jobId)) {
            throw new BizException("任务正在执行中，请稍后再试");
        }
        int failedItems = aiBatchJobItemMapper.countByJobIdAndStatus(jobId, ITEM_STATUS_FAILED);
        if (failedItems <= 0) {
            throw new BizException("没有失败题目可重试");
        }
        if (aiBatchJobMapper.markRetryPending(jobId) == 0) {
            throw new BizException("任务状态已变化，请刷新后重试");
        }
        aiBatchJobItemMapper.resetFailedItems(jobId);
        aiBatchJobItemMapper.resetRunningItems(jobId);
        aiBatchJobMapper.refreshCounts(jobId);
        List<AiBatchJobItem> items = aiBatchJobItemMapper.selectRecoverableItems(jobId);
        if (items.isEmpty()) {
            AiBatchJob refreshed = aiBatchJobMapper.selectOneById(jobId);
            int failed = defaultZero(refreshed != null ? refreshed.getFailCount() : 0);
            aiBatchJobMapper.markFinished(jobId, failed > 0 ? JOB_STATUS_PARTIAL_FAILED : JOB_STATUS_SUCCESS, null, LocalDateTime.now());
            return;
        }
        aiTaskExecutor.execute(() -> runBatchGenerate(jobId, items, job.getMode(), job.getOperatorId(),
                resolveBatchConcurrency(job.getConcurrency()), Objects.equals(job.getOverwrite(), 1)));
    }

    @Override
    public void cancelBatchJob(Long jobId) {
        AiBatchJob job = requireBatchJob(jobId);
        if (!Objects.equals(job.getStatus(), JOB_STATUS_PENDING)
                && !Objects.equals(job.getStatus(), JOB_STATUS_RUNNING)
                && !Objects.equals(job.getStatus(), JOB_STATUS_PAUSED)) {
            throw new BizException("只有排队中、执行中或已暂停的任务可以取消");
        }
        resumeRequestedBatchJobIds.remove(jobId);
        aiBatchJobItemMapper.cancelOpenItems(jobId, LocalDateTime.now());
        aiBatchJobMapper.cancelJob(jobId, "手动取消", LocalDateTime.now());
        aiBatchJobMapper.refreshCounts(jobId);
    }

    @Override
    public void deleteBatchJob(Long jobId) {
        AiBatchJob job = requireBatchJob(jobId);
        if (Objects.equals(job.getStatus(), JOB_STATUS_PENDING)
                || Objects.equals(job.getStatus(), JOB_STATUS_RUNNING)
                || Objects.equals(job.getStatus(), JOB_STATUS_PAUSED)) {
            throw new BizException("运行中或已暂停的任务不能删除，请先取消");
        }
        aiBatchJobItemMapper.deleteByJobId(jobId);
        aiBatchJobMapper.deleteFinishedJob(jobId);
    }

    private AiBatchJob createBatchJob(AiBatchGenerateDTO dto,
                                      String mode,
                                      Long operatorId,
                                      int totalCount,
                                      int submittedCount,
                                      int skippedCount,
                                      int concurrency,
                                      boolean overwrite) {
        AiBatchJob job = new AiBatchJob();
        job.setBankId(dto.getBankId());
        job.setMode(mode);
        job.setOverwrite(overwrite ? 1 : 0);
        job.setConcurrency(concurrency);
        job.setTotalCount(totalCount);
        job.setSubmittedCount(submittedCount);
        job.setSkippedCount(skippedCount);
        job.setSuccessCount(0);
        job.setFailCount(0);
        job.setStatus(JOB_STATUS_PENDING);
        job.setOperatorId(operatorId);
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        aiBatchJobMapper.insert(job);
        return job;
    }

    private AiBatchJob requireBatchJob(Long jobId) {
        if (jobId == null) {
            throw new BizException("任务ID不能为空");
        }
        AiBatchJob job = aiBatchJobMapper.selectOneById(jobId);
        if (job == null) {
            throw new BizException("批量任务不存在");
        }
        return job;
    }

    private List<AiBatchJobItem> createBatchJobItems(Long jobId, List<Long> questionIds) {
        List<AiBatchJobItem> items = new ArrayList<>();
        for (Long questionId : questionIds) {
            AiBatchJobItem item = new AiBatchJobItem();
            item.setJobId(jobId);
            item.setQuestionId(questionId);
            item.setStatus(ITEM_STATUS_PENDING);
            item.setRetryCount(0);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            aiBatchJobItemMapper.insert(item);
            items.add(item);
        }
        return items;
    }

    private void recoverUnfinishedBatchJobs() {
        List<AiBatchJob> jobs;
        try {
            jobs = aiBatchJobMapper.selectRecoverableJobs();
        } catch (Exception e) {
            log.error("扫描未完成AI批量任务失败", e);
            return;
        }
        if (jobs.isEmpty()) {
            return;
        }
        log.info("发现{}个未完成AI批量任务，准备恢复执行", jobs.size());
        for (AiBatchJob job : jobs) {
            aiTaskExecutor.execute(() -> recoverBatchJob(job));
        }
    }

    private void recoverBatchJob(AiBatchJob job) {
        try {
            aiBatchJobItemMapper.resetRunningItems(job.getId());
            aiBatchJobMapper.refreshCounts(job.getId());
            List<AiBatchJobItem> items = aiBatchJobItemMapper.selectRecoverableItems(job.getId());
            if (items.isEmpty()) {
                AiBatchJob refreshed = aiBatchJobMapper.selectOneById(job.getId());
                int failed = defaultZero(refreshed != null ? refreshed.getFailCount() : job.getFailCount());
                aiBatchJobMapper.markFinished(job.getId(),
                        failed > 0 ? JOB_STATUS_PARTIAL_FAILED : JOB_STATUS_SUCCESS,
                        null,
                        LocalDateTime.now());
                return;
            }
            int concurrency = resolveBatchConcurrency(job.getConcurrency());
            aiBatchJobMapper.markPendingForRecovery(job.getId(), "服务重启后自动恢复执行");
            runBatchGenerate(job.getId(), items, job.getMode(), job.getOperatorId(), concurrency, Objects.equals(job.getOverwrite(), 1));
        } catch (Exception e) {
            aiBatchJobMapper.markFinished(job.getId(), JOB_STATUS_FAILED, abbreviateError(e.getMessage()), LocalDateTime.now());
            log.error("恢复AI批量任务失败, jobId={}", job.getId(), e);
        }
    }

    private void runBatchGenerate(Long jobId,
                                  List<AiBatchJobItem> items,
                                  String mode,
                                  Long operatorId,
                                  int concurrency,
                                  boolean overwrite) {
        if (!activeBatchJobIds.add(jobId)) {
            log.info("批量AI任务已有执行器，跳过重复执行, jobId={}", jobId);
            return;
        }
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<AiBatchJobItem> pendingItems = new ArrayList<>(items);
        ExecutorService executor = null;
        int submitted = 0;
        int completed = 0;
        try {
            if (aiBatchJobMapper.markRunning(jobId, LocalDateTime.now()) == 0) {
                log.info("批量AI任务未进入执行状态，跳过执行, jobId={}", jobId);
                return;
            }
            executor = Executors.newFixedThreadPool(concurrency);
            ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<>(executor);
            while (true) {
                while (submitted - completed < concurrency && !pendingItems.isEmpty() && canDispatchBatchJob(jobId)) {
                    AiBatchJobItem item = pendingItems.remove(0);
                    completionService.submit(() -> processBatchItem(jobId, item, mode, operatorId, overwrite));
                    submitted++;
                }

                if (completed < submitted) {
                    Future<Boolean> future = completionService.take();
                    completed++;
                    if (Boolean.TRUE.equals(future.get())) {
                        success.incrementAndGet();
                    } else {
                        failed.incrementAndGet();
                    }
                    continue;
                }

                AiBatchJob currentJob = aiBatchJobMapper.selectOneById(jobId);
                if (currentJob != null && Objects.equals(currentJob.getStatus(), JOB_STATUS_PAUSED)) {
                    if (resumeRequestedBatchJobIds.remove(jobId) && canDispatchBatchJob(jobId)) {
                        continue;
                    }
                    log.info("批量AI生成已暂停, jobId={}, remaining={}", jobId, pendingItems.size());
                    return;
                }
                if (currentJob != null && Objects.equals(currentJob.getStatus(), JOB_STATUS_CANCELED)) {
                    aiBatchJobItemMapper.cancelOpenItems(jobId, LocalDateTime.now());
                    log.info("批量AI生成已取消, jobId={}", jobId);
                    return;
                }
                if (!pendingItems.isEmpty()) {
                    if (canDispatchBatchJob(jobId)) {
                        continue;
                    }
                    log.info("批量AI任务状态已变更，停止派发, jobId={}, remaining={}", jobId, pendingItems.size());
                    return;
                }
                break;
            }
            aiBatchJobMapper.refreshCounts(jobId);
            AiBatchJob latestJob = aiBatchJobMapper.selectOneById(jobId);
            int totalFailed = defaultZero(latestJob != null ? latestJob.getFailCount() : failed.get());
            int status = totalFailed > 0 ? JOB_STATUS_PARTIAL_FAILED : JOB_STATUS_SUCCESS;
            aiBatchJobMapper.markFinished(jobId, status, null, LocalDateTime.now());
            log.info("批量AI生成完成, jobId={}, mode={}, total={}, success={}, failed={}, concurrency={}",
                    jobId, mode, items.size(), success.get(), totalFailed, concurrency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            aiBatchJobMapper.markPendingForRecovery(jobId, "服务停止或线程中断，等待自动恢复执行");
            log.warn("批量AI生成被中断，已保留为可恢复状态, jobId={}, mode={}", jobId, mode, e);
        } catch (Exception e) {
            aiBatchJobMapper.markFinished(jobId, JOB_STATUS_FAILED, abbreviateError(e.getMessage()), LocalDateTime.now());
            log.error("批量AI生成任务异常, jobId={}, mode={}", jobId, mode, e);
        } finally {
            activeBatchJobIds.remove(jobId);
            if (executor != null) {
                executor.shutdownNow();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.warn("批量AI生成线程池未及时退出, jobId={}", jobId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            restartResumedBatchJobIfNeeded(jobId, mode, operatorId, concurrency, overwrite);
        }
    }

    private void restartResumedBatchJobIfNeeded(Long jobId,
                                                String mode,
                                                Long operatorId,
                                                int concurrency,
                                                boolean overwrite) {
        if (!resumeRequestedBatchJobIds.remove(jobId) || !canDispatchBatchJob(jobId)) {
            return;
        }
        List<AiBatchJobItem> items = aiBatchJobItemMapper.selectRecoverableItems(jobId);
        if (items.isEmpty()) {
            aiBatchJobMapper.refreshCounts(jobId);
            AiBatchJob latestJob = aiBatchJobMapper.selectOneById(jobId);
            int failed = defaultZero(latestJob != null ? latestJob.getFailCount() : 0);
            aiBatchJobMapper.markFinished(jobId, failed > 0 ? JOB_STATUS_PARTIAL_FAILED : JOB_STATUS_SUCCESS, null, LocalDateTime.now());
            return;
        }
        aiTaskExecutor.execute(() -> runBatchGenerate(jobId, items, mode, operatorId, concurrency, overwrite));
    }

    private boolean processBatchItem(Long jobId, AiBatchJobItem item, String mode, Long operatorId, boolean overwrite) {
        int retryCount = 0;
        Exception lastError = null;
        try {
            if (aiBatchJobItemMapper.markRunning(item.getId(), LocalDateTime.now()) == 0) {
                return false;
            }
            if (!canWriteBatchItem(jobId, item.getId())) {
                return false;
            }
            while (retryCount <= 1) {
                Question current = questionMapper.selectOneById(item.getQuestionId());
                if (!overwrite && current != null && shouldSkipForMode(current, mode)) {
                    return markBatchItemSuccess(jobId, item.getId(), retryCount);
                }
                generate(item.getQuestionId(), mode, operatorId, overwrite,
                        () -> canWriteBatchItem(jobId, item.getId()));
                return markBatchItemSuccess(jobId, item.getId(), retryCount);
            }
        } catch (Exception e) {
            lastError = e;
            while (retryCount < 1) {
                retryCount++;
                try {
                    if (!canWriteBatchItem(jobId, item.getId())) {
                        return false;
                    }
                    sleepBeforeRetry(retryCount);
                    if (!canWriteBatchItem(jobId, item.getId())) {
                        return false;
                    }
                    Question current = questionMapper.selectOneById(item.getQuestionId());
                    if (!overwrite && current != null && shouldSkipForMode(current, mode)) {
                        return markBatchItemSuccess(jobId, item.getId(), retryCount);
                    }
                    generate(item.getQuestionId(), mode, operatorId, overwrite,
                            () -> canWriteBatchItem(jobId, item.getId()));
                    return markBatchItemSuccess(jobId, item.getId(), retryCount);
                } catch (Exception retryError) {
                    lastError = retryError;
                }
            }
        }
        String errorMsg = lastError != null ? lastError.getMessage() : "未知错误";
        markBatchItemFailed(jobId, item.getId(), retryCount, abbreviateError(errorMsg));
        log.error("批量AI生成失败, jobId={}, questionId={}, mode={}, retryCount={}",
                jobId, item.getQuestionId(), mode, retryCount, lastError);
        return false;
    }

    private boolean markBatchItemSuccess(Long jobId, Long itemId, int retryCount) {
        if (aiBatchJobItemMapper.markSuccess(itemId, retryCount, LocalDateTime.now()) == 0) {
            return false;
        }
        aiBatchJobMapper.incrementSuccess(jobId);
        return true;
    }

    private void markBatchItemFailed(Long jobId, Long itemId, int retryCount, String errorMsg) {
        if (aiBatchJobItemMapper.markFailed(itemId, retryCount, errorMsg, LocalDateTime.now()) > 0) {
            aiBatchJobMapper.incrementFail(jobId);
        }
    }

    private boolean canWriteBatchItem(Long jobId, Long itemId) {
        AiBatchJob job = aiBatchJobMapper.selectOneById(jobId);
        if (job == null
                || (!Objects.equals(job.getStatus(), JOB_STATUS_PENDING)
                && !Objects.equals(job.getStatus(), JOB_STATUS_RUNNING)
                && !Objects.equals(job.getStatus(), JOB_STATUS_PAUSED))) {
            return false;
        }
        AiBatchJobItem item = aiBatchJobItemMapper.selectOneById(itemId);
        return item != null && Objects.equals(item.getStatus(), ITEM_STATUS_RUNNING);
    }

    private boolean canDispatchBatchJob(Long jobId) {
        AiBatchJob job = aiBatchJobMapper.selectOneById(jobId);
        return job != null && (Objects.equals(job.getStatus(), JOB_STATUS_PENDING)
                || Objects.equals(job.getStatus(), JOB_STATUS_RUNNING));
    }

    private int resolveBatchConcurrency(Integer requestedConcurrency) {
        int defaultConcurrency = safePositive(aiBatchProperties.getBatchConcurrency(), 5);
        int maxConcurrency = safePositive(aiBatchProperties.getBatchMaxConcurrency(), 10);
        if (maxConcurrency < 1) {
            maxConcurrency = 10;
        }
        int concurrency = requestedConcurrency == null ? defaultConcurrency : requestedConcurrency;
        if (concurrency < 1) {
            concurrency = 1;
        }
        return Math.min(concurrency, maxConcurrency);
    }

    private int safePositive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private void sleepBeforeRetry(int retryCount) {
        try {
            Thread.sleep(Math.min(1000L * retryCount, 3000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private AiBatchJobVO toBatchJobVO(AiBatchJob job, boolean includeFailedItems) {
        AiBatchJobVO vo = BeanUtil.copyProperties(job, AiBatchJobVO.class);
        int pendingCount = aiBatchJobItemMapper.countByJobIdAndStatus(job.getId(), ITEM_STATUS_PENDING);
        int runningCount = aiBatchJobItemMapper.countByJobIdAndStatus(job.getId(), ITEM_STATUS_RUNNING);
        vo.setPendingCount(pendingCount);
        vo.setRunningCount(runningCount);
        vo.setStatusText(batchJobStatusText(job.getStatus()));
        int submitted = defaultZero(job.getSubmittedCount());
        int done = defaultZero(job.getSuccessCount()) + defaultZero(job.getFailCount());
        vo.setProgressPercent(submitted <= 0 ? 100 : Math.min(100, (int) Math.round(done * 100.0 / submitted)));
        if (job.getOperatorId() != null) {
            Admin admin = adminMapper.selectOneById(job.getOperatorId());
            vo.setOperatorName(admin != null ? admin.getNickname() : null);
        }
        if (includeFailedItems) {
            vo.setRecentFailedItems(aiBatchJobItemMapper.selectRecentFailedItems(job.getId(), 20).stream()
                    .map(this::toBatchJobItemVO)
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private AiBatchJobItemVO toBatchJobItemVO(AiBatchJobItem item) {
        AiBatchJobItemVO vo = BeanUtil.copyProperties(item, AiBatchJobItemVO.class);
        vo.setStatusText(batchJobItemStatusText(item.getStatus()));
        return vo;
    }

    private String batchJobStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case JOB_STATUS_PENDING -> "排队中";
            case JOB_STATUS_RUNNING -> "执行中";
            case JOB_STATUS_SUCCESS -> "已完成";
            case JOB_STATUS_PARTIAL_FAILED -> "完成但有失败";
            case JOB_STATUS_FAILED -> "执行异常";
            case JOB_STATUS_PAUSED -> "已暂停";
            case JOB_STATUS_CANCELED -> "已取消";
            default -> "未知";
        };
    }

    private String batchJobItemStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case ITEM_STATUS_PENDING -> "待处理";
            case ITEM_STATUS_RUNNING -> "处理中";
            case ITEM_STATUS_SUCCESS -> "成功";
            case ITEM_STATUS_FAILED -> "失败";
            case ITEM_STATUS_CANCELED -> "已取消";
            default -> "未知";
        };
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String abbreviateError(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }

    private boolean shouldSkipForMode(Question question, String mode) {
        return switch (mode) {
            case "GENERATE_ANALYSIS" -> hasText(question.getAnalysis());
            case "GENERATE_ANSWER" -> hasText(question.getAnswer());
            case "GENERATE_BOTH" -> hasText(question.getAnswer()) || hasText(question.getAnalysis());
            default -> false;
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public PageResult<AiCallLogVO> getLogList(AiLogQueryDTO query) {
        QueryWrapper qw = QueryWrapper.create();
        // 筛选条件
        if (query.getCallType() != null && !query.getCallType().isEmpty()) {
            qw.and(AI_CALL_LOG.CALL_TYPE.eq(query.getCallType()));
        }
        if (query.getStatus() != null) {
            qw.and(AI_CALL_LOG.STATUS.eq(query.getStatus()));
        }
        if (query.getMode() != null && !query.getMode().isEmpty()) {
            qw.and(AI_CALL_LOG.MODE.eq(query.getMode()));
        }
        qw.orderBy(AI_CALL_LOG.CREATE_TIME, false);

        Page<AiCallLog> page = aiCallLogMapper.paginate(Page.of(query.getPageNum(), query.getPageSize()), qw);

        // 转换为VO并填充昵称
        List<AiCallLogVO> voList = page.getRecords().stream().map(log -> {
            AiCallLogVO vo = BeanUtil.copyProperties(log, AiCallLogVO.class);
            // 根据调用类型查询昵称
            if ("ADMIN".equals(log.getCallType()) && log.getOperatorId() != null) {
                Admin admin = adminMapper.selectOneById(log.getOperatorId());
                vo.setOperatorName(admin != null ? admin.getNickname() : null);
            } else if ("USER".equals(log.getCallType()) && log.getUserId() != null) {
                User user = userMapper.selectOneById(log.getUserId());
                vo.setOperatorName(user != null ? user.getNickname() : null);
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotalRow(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public AiStatsVO getAiStats() {
        AiStatsVO vo = new AiStatsVO();
        vo.setTotalCalls(aiCallLogMapper.selectCountByQuery(QueryWrapper.create()));
        vo.setSuccessCalls(aiCallLogMapper.selectCountByQuery(
                QueryWrapper.create().where(AI_CALL_LOG.STATUS.eq(1))));
        vo.setFailCalls(aiCallLogMapper.selectCountByQuery(
                QueryWrapper.create().where(AI_CALL_LOG.STATUS.eq(0))));
        vo.setTodayCalls(aiCallLogMapper.selectCountByQuery(
                QueryWrapper.create().where(AI_CALL_LOG.CREATE_TIME.ge(LocalDate.now().atStartOfDay()))));
        String totalTokens = aiCallLogMapper.selectObjectByQueryAs(
                QueryWrapper.create().select(sum(AI_CALL_LOG.TOKENS_USED)), String.class);
        vo.setTotalTokens(parseLongOrZero(totalTokens));
        return vo;
    }

    private String callAi(AiConfig config, String prompt) {
        return callAiWithRoute(config, prompt).content();
    }

    private AiTextResult callAiWithRoute(AiConfig config, String prompt) {
        validateConfig(config);
        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", prompt));
        String url = AiProviderUtil.buildChatCompletionsUrl(config.getProvider(), config.getBaseUrl());
        return callAiByChatCompletions(config, url, messages);
    }

    private AiTextResult callAiByChatCompletions(AiConfig config, String url, List<Map<String, String>> messages) {
        JSONObject body = buildChatBody(config, messages, true);
        String responseBody = executeChatRequest(url, config.getApiKey(), body, true);
        try {
            String content = parseChatContent(responseBody);
            log.info("AI调用链路 route=chat, model={}, baseUrl={}", config.getModel(), config.getBaseUrl());
            return new AiTextResult(content, "chat");
        } catch (BizException e) {
            if (!shouldRetryWithMinimalBody(e, body)) {
                throw e;
            }
            log.warn("AI Chat Completions 返回空内容，改用最小请求体重试一次，model={}, baseUrl={}",
                    config.getModel(), config.getBaseUrl());
            JSONObject minimalBody = buildChatBody(config, messages, false);
            String retriedResponse = executeChatRequest(url, config.getApiKey(), minimalBody, false);
            String content = parseChatContent(retriedResponse);
            log.info("AI调用链路 route=chat, model={}, baseUrl={}", config.getModel(), config.getBaseUrl());
            return new AiTextResult(content, "chat");
        }
    }

    private JSONObject buildChatBody(AiConfig config, List<Map<String, String>> messages, boolean includeOptionalParams) {
        JSONObject body = new JSONObject();
        body.set("model", config.getModel());
        body.set("messages", messages);
        if (includeOptionalParams) {
            if (config.getMaxTokens() != null && config.getMaxTokens() > 0) {
                if (preferMaxCompletionTokens(config.getModel())) {
                    body.set("max_completion_tokens", config.getMaxTokens());
                } else {
                    body.set("max_tokens", config.getMaxTokens());
                }
            }
            if (config.getTemperature() != null && supportTemperature(config.getModel())) {
                body.set("temperature", config.getTemperature());
            }
        }
        return body;
    }

    private String executeChatRequest(String url, String apiKey, JSONObject body, boolean allowCompatibilityRetry) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "quiz-ai-online/1.0")
                .post(RequestBody.create(
                        body.toString().getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("application/json")
                ))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                if (allowCompatibilityRetry && response.code() == 400) {
                    JSONObject minimalBody = new JSONObject();
                    minimalBody.set("model", body.getStr("model"));
                    minimalBody.set("messages", body.get("messages"));
                    return executeChatRequest(url, apiKey, minimalBody, false);
                }
                throw new BizException(buildApiErrorMessage("AI API返回错误", response.code(), responseBody));
            }
            return responseBody;
        } catch (IOException e) {
            throw new BizException("AI API调用异常: " + e.getMessage());
        }
    }

    private String parseChatContent(String responseBody) {
        log.debug("AI API响应: {}", responseBody);
        JSONObject json = JSONUtil.parseObj(responseBody);

        String errorMsg = extractApiErrorMessage(json);
        if (errorMsg != null) {
            throw new BizException("AI API错误: " + errorMsg);
        }

        cn.hutool.json.JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BizException("AI API返回数据异常: choices为空");
        }
        JSONObject firstChoice = choices.getJSONObject(0);
        if (firstChoice == null) {
            throw new BizException("AI API返回数据异常: choice为空");
        }
        String content = extractChoiceContent(firstChoice);
        if (!hasText(content)) {
            String reasoningContent = extractReasoningContent(firstChoice);
            if (hasText(reasoningContent)) {
                throw new BizException("AI API未返回最终答复，只返回了推理内容");
            }
            log.warn("AI API返回内容为空，响应片段: {}", abbreviateResponse(responseBody));
            throw new BizException("AI API返回内容为空");
        }
        return content.trim();
    }

    private String extractChoiceContent(JSONObject choice) {
        if (choice == null) {
            return null;
        }
        String directText = choice.getStr("text");
        if (hasText(directText)) {
            return directText;
        }
        JSONObject message = choice.getJSONObject("message");
        if (message != null) {
            String messageContent = extractContentValue(message.get("content"));
            if (hasText(messageContent)) {
                return messageContent;
            }
        }
        JSONObject delta = choice.getJSONObject("delta");
        if (delta != null) {
            String deltaContent = extractContentValue(delta.get("content"));
            if (hasText(deltaContent)) {
                return deltaContent;
            }
        }
        return null;
    }

    private String extractReasoningContent(JSONObject choice) {
        if (choice == null) {
            return null;
        }
        JSONObject message = choice.getJSONObject("message");
        if (message != null) {
            String reasoningContent = extractContentValue(message.get("reasoning_content"));
            if (hasText(reasoningContent)) {
                return reasoningContent;
            }
        }
        return extractContentValue(choice.get("reasoning_content"));
    }

    private String extractContentValue(Object rawContent) {
        if (rawContent == null) {
            return null;
        }
        if (rawContent instanceof CharSequence sequence) {
            return sequence.toString();
        }
        if (rawContent instanceof JSONArray array) {
            List<String> parts = new ArrayList<>();
            for (Object item : array) {
                String part = extractContentValue(item);
                if (hasText(part)) {
                    parts.add(part.trim());
                }
            }
            return parts.isEmpty() ? null : String.join("\n", parts);
        }
        if (rawContent instanceof JSONObject object) {
            String text = firstNonBlank(
                    extractContentValue(object.get("text")),
                    extractContentValue(object.get("content")),
                    object.getStr("value"),
                    object.getStr("output_text")
            );
            if (hasText(text)) {
                return text;
            }
            return null;
        }
        if (rawContent instanceof Number || rawContent instanceof Boolean) {
            return String.valueOf(rawContent);
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String abbreviateResponse(String responseBody) {
        if (responseBody == null) {
            return "";
        }
        String normalized = responseBody.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 300) {
            return normalized;
        }
        return normalized.substring(0, 300) + "...";
    }

    private String extractApiErrorMessage(JSONObject json) {
        if (json == null || !json.containsKey("error")) {
            return null;
        }
        Object rawError = json.get("error");
        if (rawError == null) {
            return null;
        }
        if (rawError instanceof JSONObject errorObject) {
            String message = firstNonBlank(
                    errorObject.getStr("message"),
                    errorObject.getStr("code"),
                    errorObject.toString()
            );
            return hasText(message) ? message : null;
        }
        if (rawError instanceof CharSequence sequence) {
            String message = sequence.toString().trim();
            return message.isEmpty() ? null : message;
        }
        String message = String.valueOf(rawError).trim();
        return message.isEmpty() || "null".equalsIgnoreCase(message) ? null : message;
    }

    private boolean shouldRetryWithMinimalBody(BizException exception, JSONObject body) {
        if (exception == null || body == null) {
            return false;
        }
        boolean hasOptionalParams = body.containsKey("max_completion_tokens")
                || body.containsKey("max_tokens")
                || body.containsKey("temperature");
        if (!hasOptionalParams) {
            return false;
        }
        String message = exception.getMessage();
        return message != null && message.contains("AI API返回内容为空");
    }

    private record AiTextResult(String content, String route) {
    }

    private boolean preferMaxCompletionTokens(String model) {
        if (model == null) {
            return false;
        }
        String normalized = model.trim().toLowerCase();
        return normalized.startsWith("gpt-5")
                || normalized.startsWith("o1")
                || normalized.startsWith("o3")
                || normalized.startsWith("o4");
    }

    private boolean supportTemperature(String model) {
        if (model == null) {
            return true;
        }
        String normalized = model.trim().toLowerCase();
        return !(normalized.startsWith("gpt-5")
                || normalized.startsWith("o1")
                || normalized.startsWith("o3")
                || normalized.startsWith("o4"));
    }

    private String buildApiErrorMessage(String prefix, int statusCode, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return prefix + ": " + statusCode;
        }
        try {
            JSONObject json = JSONUtil.parseObj(responseBody);
            if (json.containsKey("error")) {
                JSONObject error = json.getJSONObject("error");
                if (error != null) {
                    String msg = error.getStr("message");
                    if (msg != null && !msg.isBlank()) {
                        return prefix + ": " + statusCode + " - " + msg;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return prefix + ": " + statusCode + " - " + responseBody;
    }

    private void validateConfig(AiConfig config) {
        if (config == null) {
            throw new BizException("AI配置不存在");
        }
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new BizException("请先配置AI API Key");
        }
        if (config.getModel() == null || config.getModel().trim().isEmpty()) {
            throw new BizException("请先配置AI模型");
        }
        String baseUrl = AiProviderUtil.normalizeBaseUrl(config.getProvider(), config.getBaseUrl());
        if (baseUrl.isEmpty()) {
            throw new BizException("请先配置AI Base URL");
        }
        config.setBaseUrl(baseUrl);
        config.setProvider(AiProviderUtil.normalizeProvider(config.getProvider()));
    }

    private AiConfig buildConfigForRequest(AiConfigDTO dto) {
        if (dto == null) {
            throw new BizException("请求参数不能为空");
        }
        AiConfig config = new AiConfig();
        String provider = AiProviderUtil.normalizeProvider(dto.getProvider());
        config.setProvider(provider);
        config.setBaseUrl(AiProviderUtil.normalizeBaseUrl(provider, dto.getBaseUrl()));
        config.setApiKey(dto.getApiKey() == null ? null : dto.getApiKey().trim());
        config.setModel(dto.getModel() == null ? "" : dto.getModel().trim());
        validateConfigForModels(config);
        return config;
    }

    private void validateConfigForModels(AiConfig config) {
        if (config == null) {
            throw new BizException("AI配置不存在");
        }
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new BizException("请先填写 API Key");
        }
        String baseUrl = AiProviderUtil.normalizeBaseUrl(config.getProvider(), config.getBaseUrl());
        if (baseUrl.isEmpty()) {
            throw new BizException("请先填写 Base URL");
        }
        config.setProvider(AiProviderUtil.normalizeProvider(config.getProvider()));
        config.setBaseUrl(baseUrl);
    }

    private AiConfig mergeWithStoredConfig(AiConfigDTO dto) {
        AiConfig base = getConfig();
        AiConfig config = new AiConfig();

        String provider = dto.getProvider();
        if (provider == null || provider.trim().isEmpty()) {
            provider = base != null ? base.getProvider() : null;
        }
        config.setProvider(AiProviderUtil.normalizeProvider(provider));

        String baseUrl = dto.getBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = base != null ? base.getBaseUrl() : null;
        }
        config.setBaseUrl(AiProviderUtil.normalizeBaseUrl(config.getProvider(), baseUrl));

        String apiKey = dto.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = base != null ? base.getApiKey() : null;
        }
        config.setApiKey(apiKey == null ? null : apiKey.trim());

        String model = dto.getModel();
        if (model == null || model.trim().isEmpty()) {
            model = base != null ? base.getModel() : null;
        }
        config.setModel(model == null ? null : model.trim());

        Integer maxTokens = dto.getMaxTokens() != null ? dto.getMaxTokens() : (base != null ? base.getMaxTokens() : null);
        config.setMaxTokens(maxTokens);
        config.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : (base != null ? base.getTemperature() : null));
        config.setStatus(base != null ? base.getStatus() : 1);

        return config;
    }

    private void parseBothResult(Question question, List<QuestionOption> options, String result) {
        String normalizedResult = normalizeLineSeparators(result);
        String[] lines = normalizedResult.split("\n");
        StringBuilder analysis = new StringBuilder();
        boolean inAnalysis = false;
        for (String line : lines) {
            String trimmed = stripMarkdownLineDecorations(line).trim();
            if (trimmed.matches("^答案\\s*[:：].*")) {
                String answer = trimmed.replaceFirst("^答案\\s*[:：]\\s*", "");
                question.setAnswer(normalizeGeneratedAnswer(question, options, answer));
                inAnalysis = false;
            } else if (trimmed.matches("^(?:答案解析|题目解析|解析)\\s*[:：].*")) {
                inAnalysis = true;
                appendAnalysisLine(analysis,
                        trimmed.replaceFirst("^(?:答案解析|题目解析|解析)\\s*[:：]\\s*", ""));
            } else if (inAnalysis) {
                appendAnalysisLine(analysis, line);
            }
        }
        String normalizedAnalysis = normalizeAnalysisText(analysis.toString());
        if (hasText(normalizedAnalysis)) {
            question.setAnalysis(normalizedAnalysis);
        }
    }

    private String buildBothResult(Question question) {
        List<String> lines = new ArrayList<>();
        if (hasText(question.getAnswer())) {
            lines.add("答案：" + question.getAnswer().trim());
        }
        if (hasText(question.getAnalysis())) {
            lines.add(question.getAnalysis().trim());
        }
        return String.join("\n", lines);
    }

    private String buildQuestionContext(Question question, String optionsStr, String mode, boolean overwrite) {
        StringBuilder builder = new StringBuilder();
        builder.append("题型：").append(getQuestionTypeName(question.getType()));
        builder.append("\n题目：").append(question.getContent());
        if (hasText(optionsStr)) {
            builder.append("\n选项：\n").append(optionsStr);
        }
        if (shouldIncludeExistingAnswer(mode, overwrite) && hasText(question.getAnswer())) {
            builder.append("\n正确答案：").append(question.getAnswer().trim());
        }
        if (shouldIncludeExistingAnalysis(mode, overwrite) && hasText(question.getAnalysis())) {
            builder.append("\n已有解析：").append(question.getAnalysis().trim());
        }
        return builder.toString();
    }

    private boolean shouldIncludeExistingAnswer(String mode, boolean overwrite) {
        return !overwrite || "GENERATE_ANALYSIS".equals(mode);
    }

    private boolean shouldIncludeExistingAnalysis(String mode, boolean overwrite) {
        return !overwrite;
    }

    private String appendQuestionTypeInstruction(String prompt, Question question, String mode) {
        StringBuilder builder = new StringBuilder(prompt == null ? "" : prompt.trim());
        builder.append("\n\n题型要求：\n");
        if (question != null && (Objects.equals(question.getType(), 1) || Objects.equals(question.getType(), 2))) {
            builder.append("本题是").append(getQuestionTypeName(question.getType())).append("。");
            if ("GENERATE_ANSWER".equals(mode)) {
                if (Objects.equals(question.getType(), 1)) {
                    builder.append("只输出一个正确选项字母，不要输出解析。");
                } else {
                    builder.append("只输出正确选项字母，多个答案用英文逗号分隔，不要输出解析。");
                }
            } else {
                builder.append("解析必须逐个说明每个选项的原因，按 A、B、C、D 等选项顺序分别说明为什么正确或错误；不要只解释正确答案。");
            }
        } else if (question != null && Objects.equals(question.getType(), 3)) {
            builder.append("本题是判断题。");
            if ("GENERATE_ANSWER".equals(mode)) {
                builder.append("只输出与判断结果匹配的选项字母，不要输出解析；必须根据选项内容判断哪个字母表示正确或错误，不要假设 A 或 B 的含义。");
            } else {
                builder.append("解析只需要说明判断为对或错的依据，不需要按选项逐项分析；如输出答案，必须输出与选项内容匹配的选项字母，不要假设 A 或 B 的含义。");
            }
        } else if (question != null && Objects.equals(question.getType(), 4)) {
            builder.append("本题是填空题。");
            if ("GENERATE_ANSWER".equals(mode)) {
                builder.append("只输出应填内容，不要输出解析。");
            } else {
                builder.append("解析只需要说明填入该答案的依据、关键词或推导过程，不需要按选项逐项分析。");
            }
        } else {
            builder.append("请根据题型输出内容。选择题解析需要逐项说明每个选项的原因；判断题和填空题解析只说明原因。");
        }
        if ("GENERATE_ANALYSIS".equals(mode) || "GENERATE_BOTH".equals(mode)) {
            builder.append("\n\n输出格式要求：只输出纯文本；不要使用 Markdown 标题、加粗、列表符号、代码块或空行；解析正文不要以“解析：”“答案解析：”“题目解析：”开头。选择题选项说明直接写为 A. ...、B. ...。");
        }
        return builder.toString();
    }

    private String normalizeAnalysisText(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        text = normalizeLineSeparators(text);
        List<String> lines = new ArrayList<>();
        for (String rawLine : text.split("\n")) {
            String line = normalizeAnalysisLine(rawLine);
            if (!hasText(line) || isAnalysisHeadingOnly(line)) {
                continue;
            }
            lines.add(line);
        }
        String normalized = String.join("\n", lines);
        return trimToNull(normalized);
    }

    private String normalizeAnalysisLine(String rawLine) {
        String line = stripMarkdownLineDecorations(rawLine);
        line = stripLeadingAnalysisLabel(line);
        line = unwrapMarkdownInline(line);
        line = line.replaceAll("[ \\t\\x0B\\f]+", " ");
        return line.trim();
    }

    private String stripMarkdownLineDecorations(String rawLine) {
        if (rawLine == null) {
            return "";
        }
        String line = rawLine.replace('\u00A0', ' ').trim();
        if (line.matches("^```[\\w-]*\\s*$") || line.equals("```")) {
            return "";
        }
        line = line.replaceFirst("^#{1,6}\\s*", "");
        line = line.replaceFirst("^>+\\s*", "");
        line = line.replaceFirst("^(?:[-+*]|\\d+[.)、])\\s+", "");
        line = unwrapMarkdownInline(line);
        return line.trim();
    }

    private String unwrapMarkdownInline(String value) {
        String result = value == null ? "" : value;
        String previous;
        do {
            previous = result;
            result = result.replaceAll("\\*\\*(\\S(?:[^\\n*]*?\\S)?)\\*\\*", "$1")
                    .replaceAll("`([^`\\n]+)`", "$1");
        } while (!previous.equals(result));
        return result.trim();
    }

    private String stripLeadingAnalysisLabel(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim();
        result = result.replaceFirst("^\\s*[【\\[]\\s*(?:答案解析|题目解析|解析|详解|说明)\\s*[】\\]]\\s*[:：]?\\s*", "");
        result = result.replaceFirst("^\\s*(?:答案解析|题目解析|解析|详解|说明)\\s*(?:如下)?\\s*[:：]\\s*", "");
        return result.trim();
    }

    private boolean isAnalysisHeadingOnly(String value) {
        if (value == null) {
            return true;
        }
        String line = unwrapMarkdownInline(value).trim();
        return line.matches("^(?:答案解析|题目解析|解析|详解|说明)\\s*(?:如下)?\\s*[:：]?$");
    }

    private void appendAnalysisLine(StringBuilder builder, String line) {
        String normalizedLine = normalizeAnalysisLine(line);
        if (!hasText(normalizedLine) || isAnalysisHeadingOnly(normalizedLine)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append(normalizedLine);
    }

    private String normalizeLineSeparators(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String getQuestionTypeName(Integer type) {
        if (type == null) {
            return "未知题型";
        }
        return switch (type) {
            case 1 -> "单选题";
            case 2 -> "多选题";
            case 3 -> "判断题";
            case 4 -> "填空题";
            default -> "未知题型";
        };
    }

    private String normalizeGeneratedAnswer(Question question, List<QuestionOption> options, String rawAnswer) {
        String answer = trimToNull(stripAnswerBrackets(rawAnswer));
        if (answer == null || question == null || !Objects.equals(question.getType(), 3)) {
            return answer;
        }
        String optionLabel = findJudgeOptionLabel(options, answer);
        return optionLabel != null ? optionLabel : answer;
    }

    private String findJudgeOptionLabel(List<QuestionOption> options, String answer) {
        if (options == null || options.isEmpty() || answer == null) {
            return null;
        }
        String normalizedAnswer = normalizeJudgeText(answer);
        if (normalizedAnswer == null) {
            return null;
        }
        for (QuestionOption option : options) {
            String normalizedOption = normalizeJudgeText(option.getContent());
            if (normalizedAnswer.equals(normalizedOption)) {
                return option.getLabel();
            }
        }
        return null;
    }

    private String normalizeJudgeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim()
                .replace("。", "")
                .replace(".", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "正确", "对", "是", "TRUE", "T", "YES", "Y", "√" -> "TRUE";
            case "错误", "错", "否", "FALSE", "F", "NO", "N", "×", "X" -> "FALSE";
            default -> null;
        };
    }

    private String stripAnswerBrackets(String value) {
        String answer = trimToNull(value);
        if (answer == null) {
            return null;
        }
        return answer.replaceFirst("^\\s*[\\[【（(]\\s*", "")
                .replaceFirst("\\s*[\\]】）)]\\s*$", "");
    }

    private String resolvePromptTemplate(String mode) {
        if (mode == null) {
            throw new BizException("请选择生成模式");
        }
        return switch (mode) {
            case "GENERATE_ANALYSIS" -> DEFAULT_PROMPT_ANALYSIS;
            case "GENERATE_ANSWER" -> DEFAULT_PROMPT_ANSWER;
            case "GENERATE_BOTH" -> DEFAULT_PROMPT_BOTH;
            default -> throw new BizException("不支持的模式: " + mode);
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private Long parseLongOrZero(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
