package com.quiz.config;

import cn.dev33.satoken.stp.StpInterface;
import com.quiz.entity.Admin;
import com.quiz.mapper.AdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限/角色数据加载源
 * 为 StpKit.ADMIN 的 checkRole/checkPermission 提供数据
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private AdminMapper adminMapper;

    /**
     * 返回指定账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 暂不使用细粒度权限，返回空
        return Collections.emptyList();
    }

    /**
     * 返回指定账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 只处理 admin 类型的登录
        if (!"admin".equals(loginType)) {
            return Collections.emptyList();
        }
        Long adminId = Long.parseLong(loginId.toString());
        Admin admin = adminMapper.selectOneById(adminId);
        if (admin == null || admin.getRole() == null) {
            return Collections.emptyList();
        }
        List<String> roles = new ArrayList<>();
        roles.add(admin.getRole());
        return roles;
    }
}
