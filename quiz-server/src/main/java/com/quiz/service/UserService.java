package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.dto.app.UpdateProfileDTO;
import com.quiz.dto.app.WxLoginDTO;
import com.quiz.entity.User;
import com.quiz.vo.app.StudyStatsVO;
import com.quiz.vo.app.UserInfoVO;

public interface UserService {

    User wxLogin(WxLoginDTO dto);

    User phoneLogin(String phone, String password);

    UserInfoVO getUserInfo(Long userId);

    void updateProfile(Long userId, UpdateProfileDTO dto);

    StudyStatsVO getStudyStats(Long userId);

    void updateSettings(Long userId, String settings);

    PageResult<User> list(String keyword, Integer status, Integer pageNum, Integer pageSize);

    User getById(Long id);

    void updateStatus(Long id, Integer status);

    void setVip(Long id, Integer isVip, java.time.LocalDateTime expireTime);
}
