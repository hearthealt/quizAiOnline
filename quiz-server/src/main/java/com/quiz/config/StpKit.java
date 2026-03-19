package com.quiz.config;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Sa-Token 多账号体系：APP端用户 + Admin后台管理员
 */
public class StpKit {

    private StpKit() {
    }

    /**
     * APP 端会话对象
     */
    public static final StpLogic APP = new StpLogic("app");

    /**
     * Admin 后台会话对象
     */
    public static final StpLogic ADMIN = new StpLogic("admin");
}
