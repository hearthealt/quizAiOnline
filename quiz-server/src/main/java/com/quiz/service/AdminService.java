package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.AdminCreateDTO;
import com.quiz.dto.admin.AdminLoginDTO;
import com.quiz.dto.admin.AdminUpdateDTO;
import com.quiz.dto.admin.ChangePasswordDTO;
import com.quiz.entity.Admin;
import com.quiz.vo.admin.AdminInfoVO;

public interface AdminService {

    AdminInfoVO login(AdminLoginDTO dto);

    AdminInfoVO getInfo(Long adminId);

    void changePassword(Long adminId, ChangePasswordDTO dto);

    PageResult<Admin> list(Integer pageNum, Integer pageSize);

    void create(AdminCreateDTO dto);

    void update(Long id, AdminUpdateDTO dto);

    void delete(Long id);
}
