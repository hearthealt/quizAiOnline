package com.quiz.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.quiz.common.exception.BizException;
import com.quiz.dto.app.AiChatDTO;
import com.quiz.entity.AiCallLog;
import com.quiz.entity.AiChatMessage;
import com.quiz.entity.AiConfig;
import com.quiz.mapper.AiCallLogMapper;
import com.quiz.mapper.AiChatMessageMapper;
import com.quiz.mapper.AiConfigMapper;
import com.quiz.service.AiChatService;
import com.quiz.service.SysConfigService;
import com.quiz.util.AiProviderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiConfigMapper aiConfigMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final SysConfigService sysConfigService;
    private final OkHttpClient okHttpClient;

    @Override
    public String chat(AiChatDTO dto, Long userId) {
        if (dto == null || dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            throw new BizException("请输入要咨询的问题");
        }

        AiConfig config = getConfig();
        if (config == null || config.getStatus() == null || config.getStatus() != 1) {
            throw new BizException("AI服务未启用");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        String persona = sysConfigService.getValue("aiChatPersona",
                "你是AI智能导师，回答简洁清晰，必要时分点说明。");
        if (persona != null && !persona.trim().isEmpty()) {
            messages.add(Map.of("role", "system", "content", persona.trim()));
        }
        if (dto.getHistory() != null) {
            dto.getHistory().stream()
                    .filter(m -> m != null && m.getRole() != null && m.getContent() != null)
                    .limit(6)
                    .forEach(m -> messages.add(Map.of("role", m.getRole(), "content", m.getContent())));
        }
        messages.add(Map.of("role", "user", "content", dto.getMessage().trim()));

        long startTime = System.currentTimeMillis();
        AiCallLog callLog = new AiCallLog();
        callLog.setCallType("USER");
        callLog.setMode("CHAT");
        callLog.setPrompt(dto.getMessage().trim());
        callLog.setUserId(userId);
        callLog.setCreateTime(LocalDateTime.now());

        try {
            AiChatResult result = callAi(config, messages);
            callLog.setResult(result.content());
            callLog.setTokensUsed(result.tokensUsed());
            callLog.setCostMs((int) (System.currentTimeMillis() - startTime));
            callLog.setStatus(1);
            aiCallLogMapper.insert(callLog);
            
            // 保存用户消息和AI回复到对话历史
            saveMessage(userId, "user", dto.getMessage().trim());
            saveMessage(userId, "assistant", result.content());
            
            return result.content();
        } catch (Exception e) {
            callLog.setStatus(0);
            callLog.setErrorMsg(e.getMessage());
            callLog.setCostMs((int) (System.currentTimeMillis() - startTime));
            aiCallLogMapper.insert(callLog);
            throw e;
        }
    }

    private AiConfig getConfig() {
        List<AiConfig> list = aiConfigMapper.selectAll();
        if (list.isEmpty()) {
            return null;
        }
        AiConfig config = list.get(0);
        String provider = config.getProvider();
        if (provider == null || provider.isBlank()) {
            provider = AiProviderUtil.detectProvider(config.getBaseUrl());
        } else {
            provider = AiProviderUtil.normalizeProvider(provider);
        }
        config.setProvider(provider);
        config.setBaseUrl(AiProviderUtil.normalizeBaseUrl(provider, config.getBaseUrl()));
        return config;
    }

    private AiChatResult callAi(AiConfig config, List<Map<String, String>> messages) {
        validateConfig(config);
        String url = AiProviderUtil.buildChatCompletionsUrl(config.getProvider(), config.getBaseUrl());
        JSONObject body = buildChatBody(config, messages, true);
        String responseBody = executeChatRequest(url, config.getApiKey(), body, true);
        return parseChatResult(responseBody);
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

    private AiChatResult parseChatResult(String responseBody) {
        JSONObject json = JSONUtil.parseObj(responseBody);
        if (json.containsKey("error")) {
            JSONObject error = json.getJSONObject("error");
            String errorMsg = error != null ? error.getStr("message", "Unknown error") : "Unknown error";
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
        JSONObject message = firstChoice.getJSONObject("message");
        if (message == null) {
            throw new BizException("AI API返回数据异常: message为空");
        }
        String content = message.getStr("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BizException("AI API返回内容为空");
        }
        Integer tokensUsed = null;
        if (json.containsKey("usage")) {
            JSONObject usage = json.getJSONObject("usage");
            tokensUsed = usage.getInt("total_tokens");
        }
        return new AiChatResult(content, tokensUsed);
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
        config.setProvider(AiProviderUtil.normalizeProvider(config.getProvider()));
        config.setBaseUrl(baseUrl);
    }

    private record AiChatResult(String content, Integer tokensUsed) {}
    
    private void saveMessage(Long userId, String role, String content) {
        AiChatMessage msg = new AiChatMessage();
        msg.setUserId(userId);
        msg.setRole(role);
        msg.setContent(content);
        aiChatMessageMapper.insert(msg);
    }
    
    @Override
    public List<AiChatMessage> getHistory(Long userId) {
        return aiChatMessageMapper.selectListByQuery(
            com.mybatisflex.core.query.QueryWrapper.create()
                .where("user_id = ?", userId)
                .and("deleted = 0")
                .orderBy("id", true)
                .limit(100)
        );
    }
    
    @Override
    public List<AiChatMessage> getAllHistory(Long userId) {
        return aiChatMessageMapper.selectListByQuery(
            com.mybatisflex.core.query.QueryWrapper.create()
                .where("user_id = ?", userId)
                .orderBy("id", true)
                .limit(200)
        );
    }
    
    @Override
    public void clearHistory(Long userId) {
        // 软删除：将 deleted 置为 1
        aiChatMessageMapper.updateByQuery(
            new AiChatMessage() {{ setDeleted(1); }},
            com.mybatisflex.core.query.QueryWrapper.create()
                .where("user_id = ?", userId)
                .and("deleted = 0")
        );
    }
}
