package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.EztestProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EztestProfileMapper extends BaseMapper<EztestProfile> {

    @Select("SELECT * FROM eztest_profile WHERE operator_id = #{operatorId} LIMIT 1")
    EztestProfile selectByOperatorId(@Param("operatorId") Long operatorId);

    @Update("UPDATE eztest_profile SET permit = #{permit}, update_time = NOW() WHERE operator_id = #{operatorId}")
    int updatePermit(@Param("operatorId") Long operatorId, @Param("permit") String permit);
}
