package com.aicustomer.controller;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 聊天响应 DTO
 */
@Data
@AllArgsConstructor
public class ChatResponse {
    private String sessionId;
    private String reply;
    private String mode;
}
