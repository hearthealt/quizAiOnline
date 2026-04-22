package com.quiz.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {

    Map<String, Object> getActivityOverview(@Param("startDateTime") LocalDateTime startDateTime,
                                            @Param("endDateTime") LocalDateTime endDateTime);

    long countActiveUsers(@Param("startDateTime") LocalDateTime startDateTime,
                          @Param("endDateTime") LocalDateTime endDateTime,
                          @Param("keyword") String keyword);

    List<Map<String, Object>> listActiveUsers(@Param("startDateTime") LocalDateTime startDateTime,
                                              @Param("endDateTime") LocalDateTime endDateTime,
                                              @Param("keyword") String keyword,
                                              @Param("offset") long offset,
                                              @Param("pageSize") int pageSize);

    Map<String, Object> getActiveUserSummary(@Param("startDateTime") LocalDateTime startDateTime,
                                             @Param("endDateTime") LocalDateTime endDateTime,
                                             @Param("userId") Long userId);

    long countAnswerDetails(@Param("startDateTime") LocalDateTime startDateTime,
                            @Param("endDateTime") LocalDateTime endDateTime,
                            @Param("keyword") String keyword,
                            @Param("bankId") Long bankId,
                            @Param("source") String source,
                            @Param("result") Integer result,
                            @Param("userId") Long userId);

    List<Map<String, Object>> listAnswerDetails(@Param("startDateTime") LocalDateTime startDateTime,
                                                @Param("endDateTime") LocalDateTime endDateTime,
                                                @Param("keyword") String keyword,
                                                @Param("bankId") Long bankId,
                                                @Param("source") String source,
                                                @Param("result") Integer result,
                                                @Param("userId") Long userId,
                                                @Param("offset") long offset,
                                                @Param("pageSize") int pageSize);
}
