package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.dto.app.StartExamDTO;
import com.quiz.dto.app.SubmitExamDTO;
import com.quiz.dto.app.SaveExamAnswerDTO;
import com.quiz.vo.app.ExamOngoingVO;
import com.quiz.vo.app.ExamResultVO;
import com.quiz.vo.app.ExamSessionVO;
import com.quiz.vo.app.RecordVO;

public interface ExamService {

    ExamSessionVO startExam(Long userId, StartExamDTO dto);

    ExamResultVO submitExam(Long examId, SubmitExamDTO dto, Long userId);

    ExamResultVO getResult(Long examId, Long userId);

    PageResult<RecordVO> getRecords(Long userId, Integer pageNum, Integer pageSize);

    ExamOngoingVO getOngoingExam(Long userId, Long bankId);

    ExamSessionVO getExamSession(Long userId, Long examId);

    void saveExamAnswer(Long userId, Long examId, SaveExamAnswerDTO dto);
}
