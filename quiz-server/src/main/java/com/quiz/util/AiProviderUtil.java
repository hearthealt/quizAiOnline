package com.quiz.util;

import com.quiz.common.enums.AiProvider;

public final class AiProviderUtil {

    public static final String OPENAI_BASE_URL = "https://api.openai.com/v1";
    public static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1";

    private AiProviderUtil() {
    }

    public static String normalizeProvider(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            return AiProvider.CUSTOM.name();
        }
        try {
            return AiProvider.valueOf(provider.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            return AiProvider.CUSTOM.name();
        }
    }

    public static String detectProvider(String baseUrl) {
        String normalized = trimTrailingSlash(baseUrl);
        if (normalized.isEmpty()) {
            return AiProvider.CUSTOM.name();
        }
        String lower = normalized.toLowerCase();
        if (lower.contains("api.openai.com")) {
            return AiProvider.OPENAI.name();
        }
        if (lower.contains("api.deepseek.com")) {
            return AiProvider.DEEPSEEK.name();
        }
        return AiProvider.CUSTOM.name();
    }

    public static String normalizeBaseUrl(String provider, String baseUrl) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedBaseUrl = trimTrailingSlash(baseUrl);

        if (normalizedBaseUrl.isEmpty()) {
            return switch (AiProvider.valueOf(normalizedProvider)) {
                case OPENAI -> OPENAI_BASE_URL;
                case DEEPSEEK -> DEEPSEEK_BASE_URL;
                case CUSTOM -> "";
            };
        }

        if (AiProvider.OPENAI.name().equals(normalizedProvider)
                && "https://api.openai.com".equalsIgnoreCase(normalizedBaseUrl)) {
            return OPENAI_BASE_URL;
        }
        if (AiProvider.DEEPSEEK.name().equals(normalizedProvider)
                && "https://api.deepseek.com".equalsIgnoreCase(normalizedBaseUrl)) {
            return DEEPSEEK_BASE_URL;
        }
        return normalizedBaseUrl;
    }

    public static String buildChatCompletionsUrl(String provider, String baseUrl) {
        String normalizedBaseUrl = normalizeBaseUrl(provider, baseUrl);
        if (normalizedBaseUrl.isEmpty()) {
            return "";
        }
        if (normalizedBaseUrl.endsWith("/chat/completions")) {
            return normalizedBaseUrl;
        }
        return normalizedBaseUrl + "/chat/completions";
    }

    public static String buildModelsUrl(String provider, String baseUrl) {
        String normalizedBaseUrl = normalizeBaseUrl(provider, baseUrl);
        if (normalizedBaseUrl.isEmpty()) {
            return "";
        }
        if (normalizedBaseUrl.endsWith("/models")) {
            return normalizedBaseUrl;
        }
        return normalizedBaseUrl + "/models";
    }

    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return apiKey.substring(0, Math.min(4, apiKey.length())) + "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
