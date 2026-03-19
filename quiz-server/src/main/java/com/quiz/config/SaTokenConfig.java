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

            // 系统管理接口需要超级管理员角色
            SaRouter.match(
                    "/api/admin/system/**"
            ).check(r -> StpKit.ADMIN.checkRole(CommonConstant.ROLE_SUPER_ADMIN));

        })).addPathPatterns("/api/**");
    }
}
