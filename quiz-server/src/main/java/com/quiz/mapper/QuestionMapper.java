package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    List<Question> selectQuestionsByBankIdWithOptions(@Param("bankId") Long bankId);

    List<Question> searchQuestions(@Param("keyword") String keyword,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    int countByBankId(@Param("bankId") Long bankId);
}
