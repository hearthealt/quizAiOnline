package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.QuestionBankDTO;
import com.quiz.entity.QuestionBank;
import com.quiz.vo.app.BankDetailVO;

import java.util.List;

public interface QuestionBankService {

    PageResult<QuestionBank> pageList(Long categoryId, String keyword, Integer pageNum, Integer pageSize);

    BankDetailVO getDetail(Long id, Long userId);

    void create(QuestionBankDTO dto);

    void update(Long id, QuestionBankDTO dto);

    void delete(Long id);

    List<QuestionBank> hotBanks(Integer limit);
}
