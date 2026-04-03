package com.quiz.service.impl;

import com.quiz.mapper.UserMapper;
import com.quiz.mapper.VipOrderMapper;
import com.quiz.mapper.VipPlanMapper;
import com.quiz.vo.admin.VipStatsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VipServiceImplTest {

    @Mock
    private VipPlanMapper vipPlanMapper;
    @Mock
    private VipOrderMapper vipOrderMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private VipServiceImpl vipService;

    @Test
    void getVipStats_shouldUseDatabaseAggregates() {
        when(userMapper.selectCountByQuery(any())).thenReturn(6L);
        when(vipOrderMapper.sumPaidAmount()).thenReturn(new BigDecimal("88.50"));
        when(vipOrderMapper.sumPaidAmountFrom(any())).thenReturn(new BigDecimal("12.00"));

        VipStatsVO vo = vipService.getVipStats();

        assertEquals(6L, vo.getTotalVipUsers());
        assertEquals(new BigDecimal("88.50"), vo.getTotalRevenue());
        assertEquals(new BigDecimal("12.00"), vo.getMonthRevenue());
        assertEquals(6L, vo.getActiveVipCount());
    }
}
