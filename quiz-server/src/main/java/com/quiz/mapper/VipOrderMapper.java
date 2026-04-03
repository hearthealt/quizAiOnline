package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.VipOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface VipOrderMapper extends BaseMapper<VipOrder> {

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM vip_order
            WHERE status = 1
            """)
    BigDecimal sumPaidAmount();

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM vip_order
            WHERE status = 1
              AND paid_time >= #{monthStart}
            """)
    BigDecimal sumPaidAmountFrom(@Param("monthStart") LocalDateTime monthStart);
}
