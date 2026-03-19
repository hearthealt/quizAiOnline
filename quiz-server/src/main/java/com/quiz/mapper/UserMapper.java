package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
