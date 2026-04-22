package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.UserActivityLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserActivityLogMapper extends BaseMapper<UserActivityLog> {
}
