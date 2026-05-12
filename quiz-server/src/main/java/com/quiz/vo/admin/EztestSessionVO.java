package com.quiz.vo.admin;

import lombok.Data;

@Data
public class EztestSessionVO {

    private String sessionId;

    private String name;

    private String status;

    private String statusText;

    private String time;

    private String entrySessionid;
}
