package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.EztestExportDTO;
import com.quiz.dto.admin.EztestJobImportDTO;
import com.quiz.dto.admin.EztestProfileDTO;
import com.quiz.dto.admin.EztestSessionQueryDTO;
import com.quiz.vo.admin.EztestExportSubmitVO;
import com.quiz.vo.admin.EztestJobFileVO;
import com.quiz.vo.admin.EztestJobVO;
import com.quiz.vo.admin.EztestProfileVO;
import com.quiz.vo.admin.EztestSessionVO;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public interface EztestService {

    EztestProfileVO getProfile(Long operatorId);

    EztestProfileVO saveProfile(EztestProfileDTO dto, Long operatorId);

    List<EztestSessionVO> listSessions(EztestSessionQueryDTO dto, Long operatorId);

    EztestExportSubmitVO createJob(EztestExportDTO dto, Long operatorId);

    EztestJobVO importJob(Long id, EztestJobImportDTO dto);

    EztestJobVO getJob(Long id);

    PageResult<EztestJobVO> getJobList(Integer pageNum, Integer pageSize, Integer status);

    PageResult<EztestJobFileVO> getJobFiles(Long id, Integer pageNum, Integer pageSize);

    void downloadFile(Long jobId, Long fileId, HttpServletResponse response) throws IOException;

    void deleteJob(Long id);

    void batchDeleteJobs(List<Long> ids);
}
