package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.PracticeDetail;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface PracticeDetailMapper extends BaseMapper<PracticeDetail> {

    @Select("""
            SELECT
                COUNT(*) AS totalAnswered,
                COALESCE(SUM(CASE WHEN pd.is_correct = 1 THEN 1 ELSE 0 END), 0) AS correctCount,
                COUNT(DISTINCT DATE(pd.create_time)) AS totalDays,
                COALESCE(SUM(CASE WHEN pd.create_time >= #{todayStart} THEN 1 ELSE 0 END), 0) AS todayAnswered
            FROM practice_detail pd
            JOIN practice_record pr ON pd.record_id = pr.id
            WHERE pr.user_id = #{userId}
            """)
    Map<String, Object> getStudyStats(@Param("userId") Long userId, @Param("todayStart") LocalDateTime todayStart);
}
