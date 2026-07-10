package com.aicustomer.controller.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromptRequest {
    @NotBlank(message = "提示词内容不能为空")
    private String content;
}
