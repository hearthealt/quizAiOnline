package com.quiz.common.constant;

/**
 * 通用常量
 */
public final class CommonConstant {

    private CommonConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 成功状态码
     */
    public static final int SUCCESS_CODE = 200;

    /**
     * 失败状态码
     */
    public static final int FAIL_CODE = 500;

    /**
     * 未授权状态码
     */
    public static final int UNAUTHORIZED_CODE = 401;

    /**
     * 禁止访问状态码
     */
    public static final int FORBIDDEN_CODE = 403;

    /**
     * 超级管理员角色
     */
    public static final String ROLE_SUPER_ADMIN = "super_admin";

    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "admin";

    /**
     * 普通用户角色
     */
    public static final String ROLE_USER = "user";

    /**
     * 默认头像
     */
    public static final String DEFAULT_AVATAR = "/static/default-avatar.png";

    /**
     * 是
     */
    public static final int YES = 1;

    /**
     * 否
     */
    public static final int NO = 0;

    /**
     * 删除标记 - 未删除
     */
    public static final int NOT_DELETED = 0;

    /**
     * 删除标记 - 已删除
     */
    public static final int DELETED = 1;
}
