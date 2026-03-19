package com.quiz.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 对话消息
 */
@Data
@Table("ai_chat_message")
public class AiChatMessage implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 消息角色: user/assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 是否删除: 0-未删除 1-已删除
     */
    @Column(onInsertValue = "0")
    private Integer deleted;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}
