package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.QuestionDTO;
import com.quiz.entity.Question;
import com.quiz.vo.admin.QuestionDetailVO;
import com.quiz.vo.app.QuestionListVO;
import com.quiz.vo.app.QuestionVO;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface QuestionService {

    PageResult<Question> pageList(Long bankId, Integer type, String keyword, Integer pageNum, Integer pageSize);

    PageResult<QuestionListVO> pageAppList(Long bankId, String keyword, Integer pageNum, Integer pageSize);

    QuestionDetailVO getDetail(Long id);

    QuestionVO getQuestionVO(Long id, Long userId);

    QuestionListVO getQuestionListVO(Long id);

    void create(QuestionDTO dto);

    void update(Long id, QuestionDTO dto);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    QuestionImportResult importFromExcel(Long bankId, Long categoryId, String originalFilename, InputStream inputStream);

    QuestionImportResult importFromConverted(Long bankId, List<Map<String, Object>> questions);

    List<Question> listByBankId(Long bankId);

    class QuestionImportResult {
        private int successCount;
        private int failCount;
        private List<String> errors;

        public QuestionImportResult() {}

        public QuestionImportResult(int successCount, int failCount, List<String> errors) {
            this.successCount = successCount;
            this.failCount = failCount;
            this.errors = errors;
        }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailCount() { return failCount; }
        public void setFailCount(int failCount) { this.failCount = failCount; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
}
