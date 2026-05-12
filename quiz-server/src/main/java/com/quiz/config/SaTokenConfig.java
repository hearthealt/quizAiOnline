package com.quiz.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import com.quiz.common.constant.CommonConstant;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 权限拦截器配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {

            // APP 端接口鉴权
            SaRouter.match(
                    "/api/app/practice/**",
                    "/api/app/exam/**",
                    "/api/app/wrong/**",
                    "/api/app/favorite/**",
                    "/api/app/ai/**"
            ).check(r -> StpKit.APP.checkLogin());

            // Admin 后台接口鉴权
            SaRouter.match("/api/admin/**")
                    .notMatch("/api/admin/auth/login")
                    .check(r -> StpKit.ADMIN.checkLogin());

            // 普通 admin 仅开放仪表盘和题库/EZTest运营相关接口，其余后台模块需要超级管理员。
            SaRouter.match(
                    "/api/admin/**"
            ).notMatch(
                    "/api/admin/auth/**",
                    "/api/admin/dashboard/**",
                    "/api/admin/category/**",
                    "/api/admin/bank/**",
                    "/api/admin/question/**",
                    "/api/admin/eztest/**",
                    "/api/admin/upload/**",
                    "/api/admin/system/setting"
            ).check(r -> StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN));

            SaRouter.match("/api/admin/question/convert/**")
                    .check(r -> StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN));

        })).addPathPatterns("/api/**");
    }
}
