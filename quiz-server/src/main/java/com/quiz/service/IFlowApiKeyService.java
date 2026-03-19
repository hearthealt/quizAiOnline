package com.quiz.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.quiz.dto.admin.IFlowApiKeyResult;
import com.quiz.entity.AiConfig;
import com.quiz.mapper.AiConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * iFlow API Key 管理服务
 *
 * 核心逻辑：
 * 1. GET 获取当前Key状态（返回 apiKeyMask 脱敏key + expireTime + hasExpired）
 * 2. POST 重置Key（返回完整 apiKey + 新的 expireTime）
 * 3. 每次重置前先GET，如果已经不过期（被其他地方重置了），就不再重置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IFlowApiKeyService {

    private final AiConfigMapper aiConfigMapper;

    private static final String IFLOW_PLATFORM_URL = "https://platform.iflow.cn";
    private static final String IFLOW_API_KEY_ENDPOINT = "/api/openapi/apikey";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 保存配置后自动同步 API Key
     * 流程：GET检查状态 → 过期则POST重置 → 未过期则同步GET获取的apiKey和过期时间
     */
    @Transactional
    public IFlowApiKeyResult syncApiKey(AiConfig config) {
        if (config.getBxAuth() == null || config.getBxAuth().isEmpty()
                || config.getIflowName() == null || config.getIflowName().isEmpty()) {
            return null;
        }

        // 1. GET 检查当前状态
        IFlowApiKeyResult info = getApiKeyInfo(config);
        if (!info.getSuccess()) {
            config.setLastRenewResult("获取Key状态失败: " + info.getError());
            aiConfigMapper.update(config);
            return info;
        }

        // 2. 已过期 → POST重置获取新key
        if (info.getHasExpired() != null && info.getHasExpired()) {
            log.info("iFlow API Key 已过期，执行重置");
            return doReset(config);
        }

        // 3. 未过期 → 同步GET获取的apiKey和过期时间
        config.setApiKey(info.getApiKey());
        config.setExpireTime(info.getExpireTime());
        config.setLastRenewResult("Key有效，已同步");
        aiConfigMapper.update(config);
        log.info("iFlow API Key 有效，已同步，过期时间: {}", info.getExpireTime());
        return info;
    }

    /**
     * 确保 API Key 有效（AI调用前 / 定时任务检查）
     * 即将过期（1小时内）时触发：先GET检查 → 未过期则同步apiKey和过期时间 → 已过期则POST重置
     */
    @Transactional
    public IFlowApiKeyResult ensureValidApiKey(AiConfig config) {
        if (config.getBxAuth() == null || config.getBxAuth().isEmpty()
                || config.getIflowName() == null || config.getIflowName().isEmpty()) {
            return null;
        }

        // 本地判断：还没到即将过期的时间，跳过
        if (!isExpiringSoon(config)) {
            return IFlowApiKeyResult.success(config.getApiKey(), config.getExpireTime(), false);
        }

        log.info("iFlow API Key 即将过期或已过期，先GET检查最新状态...");

        // GET 检查最新状态（可能已被其他服务重置）
        IFlowApiKeyResult info = getApiKeyInfo(config);
        if (!info.getSuccess()) {
            log.warn("GET检查失败: {}", info.getError());
            return info;
        }

        // 未过期 → 同步apiKey和过期时间（以防其他地方重置了本地没更新）
        if (info.getHasExpired() == null || !info.getHasExpired()) {
            config.setApiKey(info.getApiKey());
            config.setExpireTime(info.getExpireTime());
            config.setLastRenewResult("Key有效，已同步");
            aiConfigMapper.update(config);
            log.info("Key有效，已同步，过期时间: {}", info.getExpireTime());
            return info;
        }

        // 已过期 → POST重置
        return doReset(config);
    }

    /**
     * 手动刷新（也遵循"先GET再POST"逻辑）
     */
    @Transactional
    public IFlowApiKeyResult refreshApiKey(AiConfig config) {
        // 先GET看看是否已被其他地方重置
        IFlowApiKeyResult info = getApiKeyInfo(config);
        if (info.getSuccess() && (info.getHasExpired() == null || !info.getHasExpired())) {
            // 未过期 → 同步apiKey和过期时间
            config.setApiKey(info.getApiKey());
            config.setExpireTime(info.getExpireTime());
            config.setLastRenewTime(LocalDateTime.now());
            config.setLastRenewResult("Key未过期，已同步");
            aiConfigMapper.update(config);
            log.info("手动刷新：Key未过期，已同步");
            return info;
        }

        // 过期了，POST重置
        return doReset(config);
    }

    /**
     * 刷新所有启用自动续期的配置（定时任务调用）
     */
    @Transactional
    public void refreshAllExpiring() {
        java.util.List<AiConfig> configs = aiConfigMapper.selectAll();
        for (AiConfig config : configs) {
            if (config.getAutoRenew() != null && config.getAutoRenew() == 1
                    && config.getBxAuth() != null && !config.getBxAuth().isEmpty()
                    && config.getIflowName() != null && !config.getIflowName().isEmpty()) {
                if (isExpiringSoon(config)) {
                    try {
                        ensureValidApiKey(config);
                    } catch (Exception e) {
                        log.error("定时任务刷新 iFlow API Key 失败: {}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 检查本地记录的过期时间是否即将过期（1小时内）
     */
    public boolean isExpiringSoon(AiConfig config) {
        if (config.getExpireTime() == null) {
            return true;
        }
        return LocalDateTime.now().plusHours(1).isAfter(config.getExpireTime());
    }

    /**
     * 获取 iFlow 平台可用模型列表
     */
    public java.util.List<java.util.Map<String, String>> getModelList(AiConfig config) {
        java.util.List<java.util.Map<String, String>> models = new java.util.ArrayList<>();
        try {
            Request request = new Request.Builder()
                    .url(IFLOW_PLATFORM_URL + "/api/platform/models/list")
                    .addHeader("Cookie", "BXAuth=" + config.getBxAuth())
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create("{}", MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("获取模型列表失败: {}", response.code());
                    return models;
                }
                String body = response.body().string();
                JSONObject root = JSONUtil.parseObj(body);
                if (!root.getBool("success", false)) {
                    return models;
                }
                JSONObject data = root.getJSONObject("data");
                if (data == null) {
                    return models;
                }
                // 遍历所有分类下的模型
                for (String key : data.keySet()) {
                    cn.hutool.json.JSONArray arr = data.getJSONArray(key);
                    if (arr == null) continue;
                    for (int i = 0; i < arr.size(); i++) {
                        JSONObject m = arr.getJSONObject(i);
                        java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
                        item.put("modelName", m.getStr("modelName"));
                        item.put("showName", m.getStr("showName"));
                        models.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取 iFlow 模型列表失败: {}", e.getMessage(), e);
        }
        return models;
    }

    // ======================== 内部方法 ========================

    /**
     * 执行POST重置，并把新Key写入数据库
     */
    private IFlowApiKeyResult doReset(AiConfig config) {
        IFlowApiKeyResult result = createApiKey(config);

        config.setLastRenewTime(LocalDateTime.now());

        if (result.getSuccess()) {
            config.setApiKey(result.getApiKey());
            config.setExpireTime(result.getExpireTime());
            config.setLastRenewResult("重置成功");
            aiConfigMapper.update(config);
            log.info("iFlow API Key 重置成功, 新过期时间: {}", result.getExpireTime());
        } else {
            config.setLastRenewResult("重置失败: " + result.getError());
            aiConfigMapper.update(config);
            log.error("iFlow API Key 重置失败: {}", result.getError());
        }

        return result;
    }

    /**
     * GET 获取当前 API Key 信息
     */
    public IFlowApiKeyResult getApiKeyInfo(AiConfig config) {
        try {
            Request request = new Request.Builder()
                    .url(IFLOW_PLATFORM_URL + IFLOW_API_KEY_ENDPOINT)
                    .addHeader("Cookie", "BXAuth=" + config.getBxAuth())
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return IFlowApiKeyResult.fail("iFlow平台返回错误: " + response.code());
                }
                String body = response.body().string();
                return parseApiKeyResponse(body);
            }
        } catch (IOException e) {
            log.error("获取 iFlow API Key 信息失败: {}", e.getMessage(), e);
            return IFlowApiKeyResult.fail("获取 API Key 信息失败: " + e.getMessage());
        }
    }

    /**
     * POST 创建/重置 API Key
     */
    private IFlowApiKeyResult createApiKey(AiConfig config) {
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.set("name", config.getIflowName());

            Request request = new Request.Builder()
                    .url(IFLOW_PLATFORM_URL + IFLOW_API_KEY_ENDPOINT)
                    .addHeader("Cookie", "BXAuth=" + config.getBxAuth())
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .post(RequestBody.create(bodyJson.toString(), MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return IFlowApiKeyResult.fail("iFlow平台返回错误: " + response.code());
                }
                String body = response.body().string();
                return parseApiKeyResponse(body);
            }
        } catch (IOException e) {
            log.error("重置 iFlow API Key 失败: {}", e.getMessage(), e);
            return IFlowApiKeyResult.fail("重置 API Key 失败: " + e.getMessage());
        }
    }

    /**
     * 解析 iFlow API 响应
     */
    private IFlowApiKeyResult parseApiKeyResponse(String response) {
        try {
            JSONObject root = JSONUtil.parseObj(response);

            boolean success = root.getBool("success", false);
            if (!success) {
                String message = root.getStr("message", "请求失败");
                return IFlowApiKeyResult.fail(message);
            }

            JSONObject data = root.getJSONObject("data");
            if (data == null) {
                return IFlowApiKeyResult.fail("响应数据为空");
            }

            // 优先取 apiKey（重置接口返回完整key），否则取 apiKeyMask（查询接口返回脱敏key）
            String apiKey = data.getStr("apiKey");
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = data.getStr("apiKeyMask");
            }

            LocalDateTime expireTime = null;
            String expireTimeStr = data.getStr("expireTime");
            if (expireTimeStr != null && !expireTimeStr.isEmpty()) {
                expireTime = parseExpireTime(expireTimeStr);
            }

            boolean hasExpired = data.getBool("hasExpired", false);
            if (!hasExpired && expireTime != null) {
                hasExpired = LocalDateTime.now().isAfter(expireTime);
            }

            if (apiKey == null || apiKey.isEmpty()) {
                return IFlowApiKeyResult.fail("响应中未找到 API Key");
            }

            return IFlowApiKeyResult.success(apiKey, expireTime, hasExpired);
        } catch (Exception e) {
            log.error("解析 iFlow API Key 响应失败: {}", e.getMessage(), e);
            return IFlowApiKeyResult.fail("解析响应失败: " + e.getMessage());
        }
    }

    private LocalDateTime parseExpireTime(String expireTimeStr) {
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ISO_DATE_TIME
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(expireTimeStr, formatter);
            } catch (Exception ignored) {
            }
        }

        log.warn("无法解析过期时间: {}", expireTimeStr);
        return null;
    }
}
