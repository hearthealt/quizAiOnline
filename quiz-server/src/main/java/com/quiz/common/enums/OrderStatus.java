package com.quiz.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * VIP订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    REJECTED(2, "已拒绝"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String desc;

    public static OrderStatus of(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的订单状态: " + code);
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isPaid() {
        return this == PAID;
    }
}
