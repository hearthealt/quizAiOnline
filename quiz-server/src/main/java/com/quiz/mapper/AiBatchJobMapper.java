package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.AiBatchJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AiBatchJobMapper extends BaseMapper<AiBatchJob> {

    @Update("UPDATE ai_batch_job SET status = 1, start_time = COALESCE(start_time, #{startTime}), update_time = NOW() WHERE id = #{id} AND status IN (0, 1)")
    int markRunning(@Param("id") Long id, @Param("startTime") LocalDateTime startTime);

    @Update("UPDATE ai_batch_job SET success_count = success_count + 1, update_time = NOW() WHERE id = #{id}")
    int incrementSuccess(@Param("id") Long id);

    @Update("UPDATE ai_batch_job SET fail_count = fail_count + 1, update_time = NOW() WHERE id = #{id}")
    int incrementFail(@Param("id") Long id);

    @Update("UPDATE ai_batch_job SET status = #{status}, error_msg = #{errorMsg}, end_time = #{endTime}, update_time = NOW() WHERE id = #{id}")
    int markFinished(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("errorMsg") String errorMsg,
                     @Param("endTime") LocalDateTime endTime);

    @Update("UPDATE ai_batch_job SET status = 0, error_msg = #{errorMsg}, update_time = NOW() WHERE id = #{id} AND status IN (0, 1)")
    int markPendingForRecovery(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    @Update("UPDATE ai_batch_job SET status = 5, error_msg = #{errorMsg}, update_time = NOW() WHERE id = #{id} AND status IN (0, 1)")
    int pauseJob(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    @Update("UPDATE ai_batch_job SET status = 0, error_msg = NULL, update_time = NOW() WHERE id = #{id} AND status = 5")
    int resumeJob(@Param("id") Long id);

    @Update("UPDATE ai_batch_job SET status = 1, error_msg = NULL, update_time = NOW() WHERE id = #{id} AND status = 5")
    int resumeActiveJob(@Param("id") Long id);

    @Update("UPDATE ai_batch_job SET status = 0, error_msg = NULL, end_time = NULL, update_time = NOW() WHERE id = #{id} AND status IN (3, 4)")
    int markRetryPending(@Param("id") Long id);

    @Update("UPDATE ai_batch_job SET status = 6, error_msg = #{errorMsg}, end_time = #{endTime}, update_time = NOW() WHERE id = #{id} AND status IN (0, 1, 5)")
    int cancelJob(@Param("id") Long id,
                  @Param("errorMsg") String errorMsg,
                  @Param("endTime") LocalDateTime endTime);

    @Update("""
            UPDATE ai_batch_job
            SET success_count = (SELECT COUNT(*) FROM ai_batch_job_item WHERE job_id = #{id} AND status = 2),
                fail_count = (SELECT COUNT(*) FROM ai_batch_job_item WHERE job_id = #{id} AND status = 3),
                update_time = NOW()
            WHERE id = #{id}
            """)
    int refreshCounts(@Param("id") Long id);

    @Select("""
            <script>
            SELECT * FROM ai_batch_job
            <where>
              <if test="status != null">AND status = #{status}</if>
              <if test="mode != null and mode != ''">AND mode = #{mode}</if>
            </where>
            ORDER BY create_time DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AiBatchJob> selectPage(@Param("offset") int offset,
                                @Param("pageSize") int pageSize,
                                @Param("status") Integer status,
                                @Param("mode") String mode);

    @Select("""
            <script>
            SELECT COUNT(*) FROM ai_batch_job
            <where>
              <if test="status != null">AND status = #{status}</if>
              <if test="mode != null and mode != ''">AND mode = #{mode}</if>
            </where>
            </script>
            """)
    long countAllJobs(@Param("status") Integer status, @Param("mode") String mode);

    @Select("SELECT * FROM ai_batch_job WHERE status IN (0, 1) ORDER BY create_time ASC")
    List<AiBatchJob> selectRecoverableJobs();

    @Delete("DELETE FROM ai_batch_job WHERE id = #{id} AND status IN (2, 3, 4, 6)")
    int deleteFinishedJob(@Param("id") Long id);
}
