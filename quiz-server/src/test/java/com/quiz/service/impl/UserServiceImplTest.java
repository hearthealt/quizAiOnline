package com.quiz.service.impl;

import com.quiz.mapper.PracticeDetailMapper;
import com.quiz.mapper.UserMapper;
import com.quiz.service.SysConfigService;
import com.quiz.vo.app.StudyStatsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PracticeDetailMapper practiceDetailMapper;
    @Mock
    private SysConfigService sysConfigService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getStudyStats_shouldUseAggregatedMapperResult() {
        when(practiceDetailMapper.getStudyStats(any(), any())).thenReturn(Map.of(
                "totalAnswered", 20L,
                "correctCount", 15L,
                "totalDays", 3L,
                "todayAnswered", 4L
        ));

        StudyStatsVO vo = userService.getStudyStats(1L);

        assertEquals(20, vo.getTotalAnswered());
        assertEquals(3, vo.getTotalDays());
        assertEquals(4, vo.getTodayAnswered());
        assertEquals(75.0, vo.getCorrectRate());
    }
}
