package com.quiz.task;

import com.quiz.service.IFlowApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * iFlow API Key 自动续期定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IFlowApiKeyTask {

    private final IFlowApiKeyService iFlowApiKeyService;

    /**
     * 每30分钟检查一次是否需要续期
     */
    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 60 * 1000)
    public void checkAndRenew() {
        log.debug("开始检查 iFlow API Key 是否需要续期...");
        try {
            iFlowApiKeyService.refreshAllExpiring();
        } catch (Exception e) {
            log.error("iFlow API Key 自动续期任务异常: {}", e.getMessage(), e);
        }
    }
}
