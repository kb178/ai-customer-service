package com.aicustomer.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 聊天请求 DTO
 */
@Data
public class ChatRequest {

    /** 用户消息（必填） */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /** 会话ID（可选，不传则自动生成） */
    private String sessionId;

    /** 对话模式：instruction 或 function（可选，默认 instruction） */
    @Pattern(regexp = "^(instruction|function)$", message = "mode 只能是 instruction 或 function")
    private String mode = "instruction";
}
