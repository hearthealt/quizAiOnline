package com.quiz.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.quiz.common.exception.BizException;
import com.quiz.entity.PracticeRecord;
import com.quiz.entity.QuestionBank;
import com.quiz.mapper.ExamAnswerMapper;
import com.quiz.mapper.ExamRecordMapper;
import com.quiz.mapper.PracticeDetailMapper;
import com.quiz.mapper.PracticeRecordMapper;
import com.quiz.mapper.QuestionBankMapper;
import com.quiz.mapper.QuestionMapper;
import com.quiz.mapper.QuestionOptionMapper;
import com.quiz.mapper.UserMapper;
import com.quiz.vo.app.RecordVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    @Mock
    private PracticeRecordMapper practiceRecordMapper;
    @Mock
    private PracticeDetailMapper practiceDetailMapper;
    @Mock
    private ExamRecordMapper examRecordMapper;
    @Mock
    private ExamAnswerMapper examAnswerMapper;
    @Mock
    private QuestionBankMapper questionBankMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private QuestionOptionMapper questionOptionMapper;
    @Mock
    private UserMapper userMapper;

    @Test
    void appRecordDetail_shouldRejectWhenPracticeRecordDoesNotBelongToCurrentUser() {
        PracticeRecord record = new PracticeRecord();
        record.setId(1L);
        record.setUserId(300L);
        when(practiceRecordMapper.selectOneById(1L)).thenReturn(record);

        RecordServiceImpl service = new RecordServiceImpl(
                practiceRecordMapper,
                practiceDetailMapper,
                examRecordMapper,
                examAnswerMapper,
                questionBankMapper,
                questionMapper,
                questionOptionMapper,
                userMapper
        );

        BizException ex = assertThrows(BizException.class, () -> service.appRecordDetail(1L, "practice", 100L));
        assertEquals(403, ex.getCode());
        assertEquals("无权访问该记录", ex.getMessage());
    }

    @Test
    void appRecordList_shouldUseDatabasePaginationForPracticeType() {
        PracticeRecord record = new PracticeRecord();
        record.setId(11L);
        record.setUserId(100L);
        record.setBankId(8L);
        record.setMode("ORDER");
        record.setTotalCount(20);
        record.setCorrectCount(18);
        record.setCreateTime(LocalDateTime.of(2026, 4, 4, 10, 0));

        Page<PracticeRecord> page = Page.of(1, 10);
        page.setRecords(List.of(record));
        page.setTotalRow(1L);

        QuestionBank bank = new QuestionBank();
        bank.setId(8L);
        bank.setName("Java题库");

        when(practiceRecordMapper.paginate(any(Page.class), any())).thenReturn(page);
        when(questionBankMapper.selectListByIds(any())).thenReturn(List.of(bank));

        RecordServiceImpl service = new RecordServiceImpl(
                practiceRecordMapper,
                practiceDetailMapper,
                examRecordMapper,
                examAnswerMapper,
                questionBankMapper,
                questionMapper,
                questionOptionMapper,
                userMapper
        );

        var result = service.appRecordList(100L, "practice", 1, 10);
        RecordVO item = result.getList().get(0);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("Java题库", item.getBankName());
        assertEquals("practice", item.getType());
        assertEquals(90.0, item.getCorrectRate());
    }
}
