package com.quiz.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * iFlow API Key 操作结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IFlowApiKeyResult {

    private Boolean success;
    private String apiKey;
    private LocalDateTime expireTime;
    private Boolean hasExpired;
    private String error;

    public static IFlowApiKeyResult success(String apiKey, LocalDateTime expireTime, Boolean hasExpired) {
        return IFlowApiKeyResult.builder()
                .success(true)
                .apiKey(apiKey)
                .expireTime(expireTime)
                .hasExpired(hasExpired)
                .build();
    }

    public static IFlowApiKeyResult fail(String error) {
        return IFlowApiKeyResult.builder()
                .success(false)
                .error(error)
                .build();
    }
}
