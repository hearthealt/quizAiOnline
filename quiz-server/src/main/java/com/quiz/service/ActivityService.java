package com.quiz.service;

import com.quiz.common.result.PageResult;

import java.util.Map;

public interface ActivityService {

    Map<String, Object> getOverview(String date);

    PageResult<Map<String, Object>> getActiveUsers(String date, String keyword, Integer pageNum, Integer pageSize);

    Map<String, Object> getUserActivityDetail(Long userId, String date, Integer timelinePageNum, Integer timelinePageSize,
                                              Integer answerPageNum, Integer answerPageSize);

    PageResult<Map<String, Object>> getAnswerDetails(String date, String keyword, Long bankId, String source,
                                                     String result, Integer pageNum, Integer pageSize);
}
