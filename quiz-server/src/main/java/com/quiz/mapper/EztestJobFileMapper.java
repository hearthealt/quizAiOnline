package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.EztestJobFile;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EztestJobFileMapper extends BaseMapper<EztestJobFile> {

    @Select("SELECT * FROM eztest_job_file WHERE job_id = #{jobId} ORDER BY id ASC")
    List<EztestJobFile> selectByJobId(@Param("jobId") Long jobId);

    @Select("SELECT * FROM eztest_job_file WHERE job_id = #{jobId} ORDER BY id ASC LIMIT #{offset}, #{pageSize}")
    List<EztestJobFile> selectPageByJobId(@Param("jobId") Long jobId,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM eztest_job_file WHERE job_id = #{jobId}")
    long countByJobId(@Param("jobId") Long jobId);

    @Select("SELECT * FROM eztest_job_file WHERE id = #{id} AND job_id = #{jobId}")
    EztestJobFile selectByIdAndJobId(@Param("id") Long id, @Param("jobId") Long jobId);

    @Delete("DELETE FROM eztest_job_file WHERE job_id = #{jobId}")
    int deleteByJobId(@Param("jobId") Long jobId);
}
