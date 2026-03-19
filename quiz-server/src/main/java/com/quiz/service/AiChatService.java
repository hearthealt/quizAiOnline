package com.quiz.service;

import com.quiz.dto.app.AiChatDTO;
import com.quiz.entity.AiChatMessage;

import java.util.List;

public interface AiChatService {
    String chat(AiChatDTO dto, Long userId);
    
    /**
     * 获取用户的对话历史（仅未删除）
     */
    List<AiChatMessage> getHistory(Long userId);
    
    /**
     * 获取用户的全部对话历史（包含已删除，后台用）
     */
    List<AiChatMessage> getAllHistory(Long userId);
    
    /**
     * 清空用户的对话历史
     */
    void clearHistory(Long userId);
}
