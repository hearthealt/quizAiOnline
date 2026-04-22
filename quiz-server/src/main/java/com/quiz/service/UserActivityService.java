package com.quiz.service;

public interface UserActivityService {

    void recordLogin(Long userId);

    void recordPracticeStart(Long userId, Long bankId, Long recordId, String mode, Integer totalCount);

    void recordPracticeAnswer(Long userId, Long bankId, Long recordId, Long questionId,
                              String userAnswer, Integer isCorrect, Integer costSeconds);

    void recordPracticeFinish(Long userId, Long bankId, Long recordId, Integer answerCount, Integer correctCount);

    void recordExamStart(Long userId, Long bankId, Long examId, Integer totalCount);

    void recordExamAnswer(Long userId, Long bankId, Long examId, Long questionId,
                          String userAnswer, Integer costSeconds);

    void recordExamSubmit(Long userId, Long bankId, Long examId, Integer totalCount, Integer correctCount, Integer score);

    void recordAiChat(Long userId, String prompt);

    void recordFavoriteAdd(Long userId, Long questionId, Long bankId);
}
