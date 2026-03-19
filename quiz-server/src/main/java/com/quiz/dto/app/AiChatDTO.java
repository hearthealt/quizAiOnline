package com.quiz.dto.app;

import lombok.Data;

import java.util.List;

@Data
public class AiChatDTO {
    private String message;
    private List<ChatMessage> history;

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
