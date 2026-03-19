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
import com.quiz.service.IFlowApiKeyService;
import com.quiz.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiConfigMapper aiConfigMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final IFlowApiKeyService iFlowApiKeyService;
    private final SysConfigService sysConfigService;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    @Override
    public String chat(AiChatDTO dto, Long userId) {
        if (dto == null || dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            throw new BizException("请输入要咨询的问题");
        }

        AiConfig config = getConfig();
        if (config == null || config.getStatus() == null || config.getStatus() != 1) {
            throw new BizException("AI服务未启用");
        }

        // 自动续期检查（如果配置了iFlow）
        if (config.getAutoRenew() != null && config.getAutoRenew() == 1) {
            iFlowApiKeyService.ensureValidApiKey(config);
            config = getConfig();
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
        return list.isEmpty() ? null : list.get(0);
    }

    private AiChatResult callAi(AiConfig config, List<Map<String, String>> messages) {
        String url = config.getBaseUrl() + "/chat/completions";

        JSONObject body = new JSONObject();
        body.set("model", config.getModel());
        body.set("messages", messages);
        body.set("max_tokens", config.getMaxTokens());
        body.set("temperature", config.getTemperature());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BizException("AI API返回错误: " + response.code());
            }
            String responseBody = response.body().string();
            JSONObject json = JSONUtil.parseObj(responseBody);
            String content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");
            Integer tokensUsed = null;
            if (json.containsKey("usage")) {
                JSONObject usage = json.getJSONObject("usage");
                tokensUsed = usage.getInt("total_tokens");
            }
            return new AiChatResult(content, tokensUsed);
        } catch (IOException e) {
            throw new BizException("AI API调用异常: " + e.getMessage());
        }
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
