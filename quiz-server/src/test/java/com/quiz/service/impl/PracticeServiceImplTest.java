package com.quiz.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.common.exception.BizException;
import com.quiz.entity.PracticeRecord;
import com.quiz.mapper.PracticeDetailMapper;
import com.quiz.mapper.PracticeRecordMapper;
import com.quiz.mapper.QuestionBankMapper;
import com.quiz.mapper.QuestionMapper;
import com.quiz.mapper.QuestionOptionMapper;
import com.quiz.service.WrongQuestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeServiceImplTest {

    @Mock
    private PracticeRecordMapper practiceRecordMapper;
    @Mock
    private PracticeDetailMapper practiceDetailMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private QuestionBankMapper questionBankMapper;
    @Mock
    private QuestionOptionMapper questionOptionMapper;
    @Mock
    private WrongQuestionService wrongQuestionService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void getQuestion_shouldRejectWhenRecordDoesNotBelongToCurrentUser() {
        PracticeRecord record = new PracticeRecord();
        record.setId(10L);
        record.setUserId(200L);
        when(practiceRecordMapper.selectOneById(10L)).thenReturn(record);

        PracticeServiceImpl service = new PracticeServiceImpl(
                practiceRecordMapper,
                practiceDetailMapper,
                questionMapper,
                questionBankMapper,
                questionOptionMapper,
                wrongQuestionService,
                stringRedisTemplate,
                new ObjectMapper()
        );

        BizException ex = assertThrows(BizException.class, () -> service.getQuestion(10L, 0, 100L));
        assertEquals(403, ex.getCode());
        assertEquals("无权访问该练习记录", ex.getMessage());
    }
}
