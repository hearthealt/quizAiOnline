package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.vo.app.RecordVO;

import java.util.Map;

public interface RecordService {

    PageResult<RecordVO> appRecordList(Long userId, String type, Integer pageNum, Integer pageSize);

    Map<String, Object> appRecordDetail(Long id, String type);

    PageResult<Map<String, Object>> adminPracticeRecords(String keyword, Long bankId, Integer pageNum, Integer pageSize);

    PageResult<Map<String, Object>> adminExamRecords(String keyword, Long bankId, Integer pageNum, Integer pageSize);
}
