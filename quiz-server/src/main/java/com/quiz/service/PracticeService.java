package com.quiz.service;

import com.quiz.dto.app.StartPracticeDTO;
import com.quiz.dto.app.SubmitAnswerDTO;
import com.quiz.entity.PracticeRecord;
import com.quiz.vo.app.PracticeResultVO;
import com.quiz.vo.app.QuestionVO;

import java.util.Map;

public interface PracticeService {

    PracticeRecord startPractice(Long userId, StartPracticeDTO dto);

    QuestionVO getQuestion(Long recordId, Integer index, Long userId);

    boolean submitAnswer(Long recordId, SubmitAnswerDTO dto, Long userId);

    PracticeResultVO finishPractice(Long recordId, Long userId);

    Map<String, Object> getProgress(Long recordId, Long userId);
}
