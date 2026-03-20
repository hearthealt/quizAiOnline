package com.quiz.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.AiBatchGenerateDTO;
import com.quiz.dto.admin.AiConfigDTO;
import com.quiz.dto.admin.AiLogQueryDTO;
import com.quiz.entity.Admin;
import com.quiz.entity.AiCallLog;
import com.quiz.entity.AiConfig;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionOption;
import com.quiz.entity.User;
import com.quiz.mapper.AdminMapper;
import com.quiz.mapper.AiCallLogMapper;
import com.quiz.mapper.AiConfigMapper;
import com.quiz.mapper.QuestionMapper;
import com.quiz.mapper.QuestionOptionMapper;
import com.quiz.mapper.UserMapper;
import com.quiz.service.AiAnalysisService;
import com.quiz.service.IFlowApiKeyService;
import com.quiz.vo.admin.AiCallLogVO;
import com.quiz.vo.admin.AiStatsVO;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import java.util.stream.Collectors;

import static com.quiz.entity.table.AiCallLogTableDef.AI_CALL_LOG;
import static com.quiz.entity.table.QuestionTableDef.QUESTION;
import static com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final AiConfigMapper aiConfigMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final IFlowApiKeyService iFlowApiKeyService;
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final OkHttpClient okHttpClient;

    @Override
    public AiConfig getConfig() {
        List<AiConfig> list = aiConfigMapper.selectAll();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void updateConfig(AiConfigDTO dto) {
        AiConfig config = getConfig();
        if (config == null) {
            config = new AiConfig();
        }
        config.setBaseUrl(dto.getBaseUrl());
        config.setApiKey(dto.getApiKey());
        config.setModel(dto.getModel());
        config.setPromptAnalysis(dto.getPromptAnalysis());
        config.setPromptAnswer(dto.getPromptAnswer());
        config.setPromptBoth(dto.getPromptBoth());
        config.setMaxTokens(dto.getMaxTokens());
        config.setTemperature(dto.getTemperature());
        if (dto.getBxAuth() != null) {
            config.setBxAuth(dto.getBxAuth());
        }
        if (dto.getIflowName() != null) {
            config.setIflowName(dto.getIflowName());
        }
        if (dto.getAutoRenew() != null) {
            config.setAutoRenew(dto.getAutoRenew());
        }
        config.setUpdateTime(LocalDateTime.now());

        if (config.getId() != null) {
            aiConfigMapper.update(config);
        } else {
            config.setCreateTime(LocalDateTime.now());
            aiConfigMapper.insert(config);
        }

        // 保存后自动同步 iFlow API Key（配置了BXAuth时）
        if (config.getBxAuth() != null && !config.getBxAuth().isEmpty()
                && config.getIflowName() != null && !config.getIflowName().isEmpty()) {
            try {
                iFlowApiKeyService.syncApiKey(config);
            } catch (Exception e) {
                log.warn("保存配置后同步iFlow API Key失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public Map<String, Object> testConnection() {
        AiConfig config = getConfig();
        if (config == null) throw new BizException("AI配置不存在");
        // 自动续期检查
        if (config.getAutoRenew() != null && config.getAutoRenew() == 1) {
            iFlowApiKeyService.ensureValidApiKey(config);
            config = getConfig();
        }

        String apiKey = config.getApiKey();
        // apiKey脱敏：显示前4位和后4位
        String maskedKey = "";
        if (apiKey != null && apiKey.length() > 8) {
            maskedKey = apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
        } else if (apiKey != null) {
            maskedKey = apiKey.substring(0, Math.min(4, apiKey.length())) + "****";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("apiKey", maskedKey);
        result.put("baseUrl", config.getBaseUrl());
        result.put("model", config.getModel());

        try {
            String reply = callAi(config, "请回复：连接成功");
            result.put("success", true);
            result.put("reply", reply);
        } catch (Exception e) {
            log.error("AI连通测试失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public String generate(Long questionId, String mode, Long operatorId) {
        AiConfig config = getConfig();
        if (config == null || config.getStatus() != 1) throw new BizException("AI服务未启用");

        // 自动续期检查（如果配置了iFlow）
        if (config.getAutoRenew() != null && config.getAutoRenew() == 1) {
            iFlowApiKeyService.ensureValidApiKey(config);
            // 重新获取最新的config（apiKey可能已更新）
            config = getConfig();
        }

        Question question = questionMapper.selectOneById(questionId);
        if (question == null) throw new BizException("题目不存在");

        List<QuestionOption> options = questionOptionMapper.selectListByQuery(
                QueryWrapper.create().where(QUESTION_OPTION.QUESTION_ID.eq(questionId))
                        .orderBy(QUESTION_OPTION.SORT, true));
        String optionsStr = options.stream()
                .map(o -> o.getLabel() + ". " + o.getContent())
                .collect(Collectors.joining("\n"));

        String promptTemplate;
        switch (mode) {
            case "GENERATE_ANALYSIS": promptTemplate = config.getPromptAnalysis(); break;
            case "GENERATE_ANSWER": promptTemplate = config.getPromptAnswer(); break;
            case "GENERATE_BOTH": promptTemplate = config.getPromptBoth(); break;
            default: throw new BizException("不支持的模式: " + mode);
        }

        String prompt = promptTemplate
                .replace("{content}", question.getContent())
                .replace("{options}", optionsStr)
                .replace("{answer}", question.getAnswer() != null ? question.getAnswer() : "")
                .replace("{analysis}", question.getAnalysis() != null ? question.getAnalysis() : "");

        long startTime = System.currentTimeMillis();
        AiCallLog callLog = new AiCallLog();
        callLog.setCallType("ADMIN");
        callLog.setQuestionId(questionId);
        callLog.setMode(mode);
        callLog.setPrompt(prompt);
        callLog.setOperatorId(operatorId);
        callLog.setCreateTime(LocalDateTime.now());

        try {
            String result = callAi(config, prompt);
            long costMs = System.currentTimeMillis() - startTime;
            callLog.setResult(result);
            callLog.setCostMs((int) costMs);
            callLog.setStatus(1);

            // 更新题目
            if ("GENERATE_ANALYSIS".equals(mode)) {
                question.setAnalysis(result);
            } else if ("GENERATE_ANSWER".equals(mode)) {
                question.setAnswer(result.trim());
            } else if ("GENERATE_BOTH".equals(mode)) {
                parseBothResult(question, result);
            }
            questionMapper.update(question);

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

    @Async
    @Override
    public void generateAsync(Long questionId, String mode, Long operatorId) {
        try {
            generate(questionId, mode, operatorId);
        } catch (Exception e) {
            log.error("异步AI生成失败, questionId={}, mode={}", questionId, mode, e);
        }
    }

    @Override
    public Map<String, Object> batchGenerate(AiBatchGenerateDTO dto, Long operatorId) {
        List<Question> questions;
        if (dto.getQuestionIds() != null && !dto.getQuestionIds().isEmpty()) {
            questions = questionMapper.selectListByIds(dto.getQuestionIds());
        } else if (dto.getBankId() != null) {
            questions = questionMapper.selectListByQuery(
                    QueryWrapper.create().where(QUESTION.BANK_ID.eq(dto.getBankId()))
                            .and(QUESTION.STATUS.eq(1)));
        } else {
            throw new BizException("请指定题库或题目");
        }

        // 异步执行每个题目的生成
        for (Question q : questions) {
            generateAsync(q.getId(), dto.getMode(), operatorId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", questions.size());
        result.put("message", "已提交" + questions.size() + "道题目的AI生成任务，后台处理中");
        return result;
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
        vo.setTodayCalls(0L);
        vo.setTotalTokens(0L);
        return vo;
    }

    private String callAi(AiConfig config, String prompt) {
        String url = config.getBaseUrl() + "/chat/completions";

        JSONObject body = new JSONObject();
        body.set("model", config.getModel());
        body.set("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.set("max_tokens", config.getMaxTokens());
        body.set("temperature", config.getTemperature());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(),
                        MediaType.parse("application/json")))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BizException("AI API返回错误: " + response.code());
            }
            String responseBody = response.body().string();
            log.debug("AI API响应: {}", responseBody);
            JSONObject json = JSONUtil.parseObj(responseBody);

            // 检查是否有错误信息
            if (json.containsKey("error")) {
                JSONObject error = json.getJSONObject("error");
                String errorMsg = error != null ? error.getStr("message", "Unknown error") : "Unknown error";
                throw new BizException("AI API错误: " + errorMsg);
            }

            // 解析返回内容
            cn.hutool.json.JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BizException("AI API返回数据异常: choices为空");
            }
            JSONObject firstChoice = choices.getJSONObject(0);
            if (firstChoice == null) {
                throw new BizException("AI API返回数据异常: choice为空");
            }
            JSONObject message = firstChoice.getJSONObject("message");
            if (message == null) {
                throw new BizException("AI API返回数据异常: message为空");
            }
            String content = message.getStr("content");
            if (content == null || content.trim().isEmpty()) {
                throw new BizException("AI API返回内容为空");
            }
            return content;
        } catch (IOException e) {
            throw new BizException("AI API调用异常: " + e.getMessage());
        }
    }

    private void parseBothResult(Question question, String result) {
        String[] lines = result.split("\n");
        StringBuilder analysis = new StringBuilder();
        boolean inAnalysis = false;
        for (String line : lines) {
            if (line.startsWith("答案：") || line.startsWith("答案:")) {
                question.setAnswer(line.substring(3).trim());
            } else if (line.startsWith("解析：") || line.startsWith("解析:")) {
                inAnalysis = true;
                analysis.append(line.substring(3).trim());
            } else if (inAnalysis) {
                analysis.append("\n").append(line);
            }
        }
        if (analysis.length() > 0) {
            question.setAnalysis(analysis.toString().trim());
        }
    }
}
