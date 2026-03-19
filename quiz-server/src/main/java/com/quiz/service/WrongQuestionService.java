package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.vo.app.WrongQuestionVO;

import java.util.Map;

public interface WrongQuestionService {

    PageResult<WrongQuestionVO> list(Long userId, Long bankId, Integer pageNum, Integer pageSize);

    Map<String, Object> stats(Long userId);

    void remove(Long userId, Long id);

    void addWrongQuestion(Long userId, Long questionId, Long bankId, String wrongAnswer);

    PageResult<Map<String, Object>> adminList(String keyword, Long bankId, Integer pageNum, Integer pageSize);
}
