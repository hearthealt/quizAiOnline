package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("vip_order")
public class VipOrder implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long planId;

    private String planName;

    private BigDecimal amount;

    private Integer duration;

    private Integer status;

    private String paymentMethod;

    private LocalDateTime paidTime;

    private LocalDateTime vipStartTime;

    private LocalDateTime vipEndTime;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
