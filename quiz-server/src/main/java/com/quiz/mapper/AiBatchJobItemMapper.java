package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.AiBatchJobItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AiBatchJobItemMapper extends BaseMapper<AiBatchJobItem> {

    @Update("UPDATE ai_batch_job_item SET status = 1, start_time = #{startTime}, update_time = NOW() WHERE id = #{id} AND status = 0")
    int markRunning(@Param("id") Long id, @Param("startTime") LocalDateTime startTime);

    @Update("UPDATE ai_batch_job_item SET status = 2, retry_count = #{retryCount}, end_time = #{endTime}, update_time = NOW() WHERE id = #{id} AND status = 1")
    int markSuccess(@Param("id") Long id,
                    @Param("retryCount") Integer retryCount,
                    @Param("endTime") LocalDateTime endTime);

    @Update("UPDATE ai_batch_job_item SET status = 3, retry_count = #{retryCount}, error_msg = #{errorMsg}, end_time = #{endTime}, update_time = NOW() WHERE id = #{id} AND status = 1")
    int markFailed(@Param("id") Long id,
                   @Param("retryCount") Integer retryCount,
                   @Param("errorMsg") String errorMsg,
                   @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM ai_batch_job_item WHERE job_id = #{jobId} AND status = #{status}")
    int countByJobIdAndStatus(@Param("jobId") Long jobId, @Param("status") Integer status);

    @Update("UPDATE ai_batch_job_item SET status = 0, update_time = NOW() WHERE job_id = #{jobId} AND status = 1")
    int resetRunningItems(@Param("jobId") Long jobId);

    @Update("""
            UPDATE ai_batch_job_item
            SET status = 0,
                retry_count = 0,
                error_msg = NULL,
                start_time = NULL,
                end_time = NULL,
                update_time = NOW()
            WHERE job_id = #{jobId} AND status = 3
            """)
    int resetFailedItems(@Param("jobId") Long jobId);

    @Update("UPDATE ai_batch_job_item SET status = 4, end_time = #{endTime}, update_time = NOW() WHERE job_id = #{jobId} AND status IN (0, 1)")
    int cancelOpenItems(@Param("jobId") Long jobId, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM ai_batch_job_item WHERE job_id = #{jobId} AND status IN (0, 1) ORDER BY id ASC")
    List<AiBatchJobItem> selectRecoverableItems(@Param("jobId") Long jobId);

    @Select("SELECT * FROM ai_batch_job_item WHERE job_id = #{jobId} AND status = 3 ORDER BY update_time DESC LIMIT #{limit}")
    List<AiBatchJobItem> selectRecentFailedItems(@Param("jobId") Long jobId, @Param("limit") int limit);

    @Delete("DELETE FROM ai_batch_job_item WHERE job_id = #{jobId}")
    int deleteByJobId(@Param("jobId") Long jobId);
}
