package com.quiz.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    int countTotalUsers();

    int countTotalQuestions();

    int countTodayActiveUsers();

    int countTodayAnswers();

    List<Map<String, Object>> getUserGrowthTrend(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> getAnswerTrend(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    List<Map<String, Object>> getBankHotRank(@Param("limit") int limit);

    List<Map<String, Object>> getWrongQuestionRank(@Param("bankId") Long bankId,
                                                    @Param("limit") int limit);

    List<Map<String, Object>> getFavoriteRank(@Param("limit") int limit);
}
