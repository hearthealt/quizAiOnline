package com.quiz.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.entity.UserActivityLog;
import com.quiz.mapper.UserActivityLogMapper;
import com.quiz.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService {

    private static final String ACTION_LOGIN = "LOGIN";
    private static final String ACTION_PRACTICE_START = "PRACTICE_START";
    private static final String ACTION_PRACTICE_ANSWER = "PRACTICE_ANSWER";
    private static final String ACTION_PRACTICE_FINISH = "PRACTICE_FINISH";
    private static final String ACTION_EXAM_START = "EXAM_START";
    private static final String ACTION_EXAM_ANSWER = "EXAM_ANSWER";
    private static final String ACTION_EXAM_SUBMIT = "EXAM_SUBMIT";
    private static final String ACTION_AI_CHAT = "AI_CHAT";
    private static final String ACTION_FAVORITE_ADD = "FAVORITE_ADD";

    private final UserActivityLogMapper userActivityLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void recordLogin(Long userId) {
        saveLog(userId, ACTION_LOGIN, "auth", null, null, null, null,
                null, null, null, null);
    }

    @Override
    public void recordPracticeStart(Long userId, Long bankId, Long recordId, String mode, Integer totalCount) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("mode", mode);
        ext.put("totalCount", totalCount);
        saveLog(userId, ACTION_PRACTICE_START, "practice", recordId, bankId, null, recordId,
                null, null, null, ext);
    }

    @Override
    public void recordPracticeAnswer(Long userId, Long bankId, Long recordId, Long questionId,
                                     String userAnswer, Integer isCorrect, Integer costSeconds) {
        saveLog(userId, ACTION_PRACTICE_ANSWER, "practice", recordId, bankId, questionId, recordId,
                userAnswer, isCorrect, costSeconds, null);
    }

    @Override
    public void recordPracticeFinish(Long userId, Long bankId, Long recordId, Integer answerCount, Integer correctCount) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("answerCount", answerCount);
        ext.put("correctCount", correctCount);
        saveLog(userId, ACTION_PRACTICE_FINISH, "practice", recordId, bankId, null, recordId,
                null, null, null, ext);
    }

    @Override
    public void recordExamStart(Long userId, Long bankId, Long examId, Integer totalCount) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("totalCount", totalCount);
        saveLog(userId, ACTION_EXAM_START, "exam", examId, bankId, null, examId,
                null, null, null, ext);
    }

    @Override
    public void recordExamAnswer(Long userId, Long bankId, Long examId, Long questionId,
                                 String userAnswer, Integer costSeconds) {
        saveLog(userId, ACTION_EXAM_ANSWER, "exam", examId, bankId, questionId, examId,
                userAnswer, null, costSeconds, null);
    }

    @Override
    public void recordExamSubmit(Long userId, Long bankId, Long examId, Integer totalCount, Integer correctCount, Integer score) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("totalCount", totalCount);
        ext.put("correctCount", correctCount);
        ext.put("score", score);
        saveLog(userId, ACTION_EXAM_SUBMIT, "exam", examId, bankId, null, examId,
                null, null, null, ext);
    }

    @Override
    public void recordAiChat(Long userId, String prompt) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("prompt", abbreviate(prompt, 120));
        saveLog(userId, ACTION_AI_CHAT, "ai", null, null, null, null,
                null, null, null, ext);
    }

    @Override
    public void recordFavoriteAdd(Long userId, Long questionId, Long bankId) {
        saveLog(userId, ACTION_FAVORITE_ADD, "favorite", questionId, bankId, questionId, null,
                null, null, null, null);
    }

    private void saveLog(Long userId, String actionType, String bizType, Long bizId,
                         Long bankId, Long questionId, Long recordId,
                         String userAnswer, Integer isCorrect, Integer costSeconds,
                         Map<String, Object> ext) {
        if (userId == null || actionType == null || actionType.isBlank()) {
            return;
        }
        UserActivityLog logEntity = new UserActivityLog();
        logEntity.setUserId(userId);
        logEntity.setActivityDate(LocalDate.now());
        logEntity.setActionType(actionType);
        logEntity.setBizType(bizType);
        logEntity.setBizId(bizId);
        logEntity.setBankId(bankId);
        logEntity.setQuestionId(questionId);
        logEntity.setRecordId(recordId);
        logEntity.setUserAnswer(userAnswer);
        logEntity.setIsCorrect(isCorrect);
        logEntity.setCostSeconds(costSeconds);
        logEntity.setExtJson(toJson(ext));
        userActivityLogMapper.insert(logEntity);
    }

    private String toJson(Map<String, Object> ext) {
        if (ext == null || ext.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ext);
        } catch (JsonProcessingException e) {
            log.warn("序列化用户行为扩展字段失败", e);
            return null;
        }
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
