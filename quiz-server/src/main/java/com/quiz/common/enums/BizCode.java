package com.quiz.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务错误码枚举
 */
@Getter
@AllArgsConstructor
public enum BizCode {

    // 通用错误 10xxx
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(10001, "参数错误"),
    UNAUTHORIZED(10002, "未登录"),
    FORBIDDEN(10003, "无权限访问"),
    NOT_FOUND(10004, "资源不存在"),
    SYSTEM_ERROR(10005, "系统繁忙"),

    // 用户相关 11xxx
    USER_NOT_FOUND(11001, "用户不存在"),
    USER_DISABLED(11002, "账号已被禁用"),
    USER_LOGIN_FAILED(11003, "手机号或密码错误"),
    REGISTER_DISABLED(11004, "系统暂未开放注册"),
    PHONE_ALREADY_EXISTS(11005, "手机号已被占用"),

    // 题库相关 20xxx
    BANK_NOT_FOUND(20001, "题库不存在"),
    BANK_NO_QUESTIONS(20002, "题库暂无题目"),

    // 题目相关 21xxx
    QUESTION_NOT_FOUND(21001, "题目不存在"),
    QUESTION_IMPORT_FAILED(21002, "题目导入失败"),

    // 考试相关 22xxx
    EXAM_NOT_FOUND(22001, "考试记录不存在"),
    EXAM_EXPIRED(22002, "考试已过期"),
    EXAM_ALREADY_SUBMITTED(22003, "已交卷，不能重复提交"),
    EXAM_NOT_STARTED(22004, "考试未开始"),

    // 练习相关 23xxx
    PRACTICE_NOT_FOUND(23001, "练习记录不存在"),
    PRACTICE_QUESTION_INDEX_OUT(23002, "题目索引超出范围"),
    WRONG_RETRY_DISABLED(23003, "错题重练功能已关闭"),

    // 收藏/错题 24xxx
    FAVORITE_NOT_FOUND(24001, "收藏记录不存在"),
    WRONG_QUESTION_NOT_FOUND(24002, "错题记录不存在"),

    // VIP相关 30xxx
    VIP_REQUIRED(30001, "需要VIP权限"),
    VIP_PLAN_NOT_FOUND(30002, "套餐不存在或已下架"),
    VIP_ORDER_NOT_FOUND(30003, "订单不存在"),
    VIP_ORDER_STATUS_ERROR(30004, "订单状态异常"),

    // AI相关 40xxx
    AI_SERVICE_DISABLED(40001, "AI服务未启用"),
    AI_CONFIG_NOT_FOUND(40002, "AI配置不存在"),
    AI_CALL_FAILED(40003, "AI调用失败"),
    AI_INVALID_MODE(40004, "不支持的AI模式");

    private final int code;
    private final String message;

    /**
     * 获取业务异常
     */
    public com.quiz.common.exception.BizException exception() {
        return new com.quiz.common.exception.BizException(this.code, this.message);
    }

    /**
     * 获取业务异常（自定义消息）
     */
    public com.quiz.common.exception.BizException exception(String customMessage) {
        return new com.quiz.common.exception.BizException(this.code, customMessage);
    }
}
