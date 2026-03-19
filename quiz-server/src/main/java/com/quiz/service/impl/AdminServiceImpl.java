package com.quiz.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.config.StpKit;
import com.quiz.dto.admin.AdminCreateDTO;
import com.quiz.dto.admin.AdminLoginDTO;
import com.quiz.dto.admin.AdminUpdateDTO;
import com.quiz.dto.admin.ChangePasswordDTO;
import com.quiz.entity.Admin;
import com.quiz.mapper.AdminMapper;
import com.quiz.service.AdminService;
import com.quiz.util.SecurityUtils;
import com.quiz.vo.admin.AdminInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.quiz.entity.table.AdminTableDef.ADMIN;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public AdminInfoVO login(AdminLoginDTO dto) {
        QueryWrapper query = QueryWrapper.create()
                .where(ADMIN.USERNAME.eq(dto.getUsername()));
        Admin admin = adminMapper.selectOneByQuery(query);

        if (admin == null) {
            throw new BizException("用户名或密码错误");
        }
        System.out.println(SecurityUtils.hashPassword(dto.getPassword()));
        if (!SecurityUtils.checkPassword(dto.getPassword(), admin.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (admin.getStatus() != 1) {
            throw new BizException("账号已被禁用");
        }

        // 登录
        StpKit.ADMIN.login(admin.getId());

        // 更新最后登录时间
        admin.setLastLoginTime(LocalDateTime.now());
        adminMapper.update(admin);

        AdminInfoVO vo = toAdminInfoVO(admin);
        vo.setToken(StpKit.ADMIN.getTokenValue());
        return vo;
    }

    @Override
    public AdminInfoVO getInfo(Long adminId) {
        Admin admin = adminMapper.selectOneById(adminId);
        if (admin == null) {
            throw new BizException("管理员不存在");
        }
        return toAdminInfoVO(admin);
    }

    @Override
    @Transactional
    public void changePassword(Long adminId, ChangePasswordDTO dto) {
        Admin admin = adminMapper.selectOneById(adminId);
        if (admin == null) {
            throw new BizException("管理员不存在");
        }
        if (!SecurityUtils.checkPassword(dto.getOldPassword(), admin.getPassword())) {
            throw new BizException("原密码错误");
        }

        admin.setPassword(SecurityUtils.hashPassword(dto.getNewPassword()));
        adminMapper.update(admin);
    }

    @Override
    public PageResult<Admin> list(Integer pageNum, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .orderBy(ADMIN.CREATE_TIME.desc());
        Page<Admin> page = adminMapper.paginate(pageNum, pageSize, query);
        return PageResult.of(page.getRecords(), page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    @Transactional
    public void create(AdminCreateDTO dto) {
        // 检查用户名唯一性
        QueryWrapper query = QueryWrapper.create()
                .where(ADMIN.USERNAME.eq(dto.getUsername()));
        long count = adminMapper.selectCountByQuery(query);
        if (count > 0) {
            throw new BizException("用户名已存在");
        }

        Admin admin = new Admin();
        admin.setUsername(dto.getUsername());
        admin.setPassword(SecurityUtils.hashPassword(dto.getPassword()));
        admin.setRole(dto.getRole());
        admin.setNickname(dto.getUsername());
        admin.setStatus(1);
        adminMapper.insert(admin);
    }

    @Override
    @Transactional
    public void update(Long id, AdminUpdateDTO dto) {
        Admin admin = adminMapper.selectOneById(id);
        if (admin == null) {
            throw new BizException("管理员不存在");
        }

        if (dto.getUsername() != null) {
            // 检查用户名唯一性（排除自身）
            QueryWrapper query = QueryWrapper.create()
                    .where(ADMIN.USERNAME.eq(dto.getUsername()))
                    .and(ADMIN.ID.ne(id));
            long count = adminMapper.selectCountByQuery(query);
            if (count > 0) {
                throw new BizException("用户名已存在");
            }
            admin.setUsername(dto.getUsername());
        }
        if (dto.getNickname() != null) {
            admin.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            admin.setAvatar(dto.getAvatar());
        }
        if (dto.getRole() != null) {
            admin.setRole(dto.getRole());
        }
        adminMapper.update(admin);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Admin admin = adminMapper.selectOneById(id);
        if (admin == null) {
            throw new BizException("管理员不存在");
        }
        adminMapper.deleteById(id);
    }

    private AdminInfoVO toAdminInfoVO(Admin admin) {
        AdminInfoVO vo = new AdminInfoVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setAvatar(admin.getAvatar());
        vo.setRole(admin.getRole());
        return vo;
    }
}
