package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
