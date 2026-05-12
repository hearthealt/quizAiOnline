package com.quiz.mapper;

import com.mybatisflex.core.BaseMapper;
import com.quiz.entity.EztestJob;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EztestJobMapper extends BaseMapper<EztestJob> {

    @Update("UPDATE eztest_job SET status = 1, start_time = #{startTime}, progress_text = #{progressText}, update_time = NOW() WHERE id = #{id} AND status = 0")
    int markRunning(@Param("id") Long id,
                    @Param("startTime") LocalDateTime startTime,
                    @Param("progressText") String progressText);

    @Update("""
            UPDATE eztest_job
            SET completed_count = #{completedCount},
                progress_percent = #{progressPercent},
                raw_count = #{rawCount},
                exported_count = #{exportedCount},
                duplicate_count = #{duplicateCount},
                progress_text = #{progressText},
                logs = #{logs},
                update_time = NOW()
            WHERE id = #{id}
            """)
    int updateProgress(@Param("id") Long id,
                       @Param("completedCount") Integer completedCount,
                       @Param("progressPercent") Integer progressPercent,
                       @Param("rawCount") Integer rawCount,
                       @Param("exportedCount") Integer exportedCount,
                       @Param("duplicateCount") Integer duplicateCount,
                       @Param("progressText") String progressText,
                       @Param("logs") String logs);

    @Update("""
            UPDATE eztest_job
            SET import_create_count = #{createCount},
                import_update_count = #{updateCount},
                import_fail_count = #{failCount},
                update_time = NOW()
            WHERE id = #{id}
            """)
    int updateImportCounts(@Param("id") Long id,
                           @Param("createCount") Integer createCount,
                           @Param("updateCount") Integer updateCount,
                           @Param("failCount") Integer failCount);

    @Update("UPDATE eztest_job SET import_bank_id = #{bankId}, import_category_id = #{categoryId}, update_time = NOW() WHERE id = #{id}")
    int updateImportTarget(@Param("id") Long id,
                           @Param("bankId") Long bankId,
                           @Param("categoryId") Long categoryId);

    @Update("UPDATE eztest_job SET import_payload = #{payload}, update_time = NOW() WHERE id = #{id}")
    int updateImportPayload(@Param("id") Long id, @Param("payload") String payload);

    @Update("UPDATE eztest_job SET progress_percent = #{progressPercent}, progress_text = #{progressText}, logs = #{logs}, update_time = NOW() WHERE id = #{id}")
    int updateStepProgress(@Param("id") Long id,
                           @Param("progressPercent") Integer progressPercent,
                           @Param("progressText") String progressText,
                           @Param("logs") String logs);

    @Update("UPDATE eztest_job SET status = #{status}, progress_text = #{progressText}, logs = #{logs}, update_time = NOW() WHERE id = #{id}")
    int updateManualImportResult(@Param("id") Long id,
                                 @Param("status") Integer status,
                                 @Param("progressText") String progressText,
                                 @Param("logs") String logs);

    @Update("UPDATE eztest_job SET session_names = #{sessionNames}, session_count = #{sessionCount}, update_time = NOW() WHERE id = #{id}")
    int updateSessions(@Param("id") Long id,
                       @Param("sessionNames") String sessionNames,
                       @Param("sessionCount") Integer sessionCount);

    @Update("UPDATE eztest_job SET status = #{status}, progress_percent = CASE WHEN #{status} IN (2, 3) THEN 100 ELSE progress_percent END, error_msg = #{errorMsg}, progress_text = #{progressText}, logs = #{logs}, end_time = #{endTime}, update_time = NOW() WHERE id = #{id}")
    int markFinished(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("errorMsg") String errorMsg,
                     @Param("progressText") String progressText,
                     @Param("logs") String logs,
                     @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT * FROM eztest_job
            <where>
              <if test="status != null">AND status = #{status}</if>
            </where>
            ORDER BY create_time DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<EztestJob> selectPage(@Param("offset") int offset,
                               @Param("pageSize") int pageSize,
                               @Param("status") Integer status);

    @Select("""
            <script>
            SELECT COUNT(*) FROM eztest_job
            <where>
              <if test="status != null">AND status = #{status}</if>
            </where>
            </script>
            """)
    long countAllJobs(@Param("status") Integer status);

    @Select("SELECT * FROM eztest_job WHERE status = 0 ORDER BY create_time ASC")
    List<EztestJob> selectPendingJobs();

    @Delete("DELETE FROM eztest_job WHERE id = #{id} AND status IN (2, 3, 4)")
    int deleteFinishedJob(@Param("id") Long id);
}
