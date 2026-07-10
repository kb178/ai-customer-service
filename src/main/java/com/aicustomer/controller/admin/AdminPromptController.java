package com.aicustomer.controller.admin;

import com.aicustomer.entity.SystemPrompt;
import com.aicustomer.service.SystemPromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/prompt")
@RequiredArgsConstructor
public class AdminPromptController {

    private final SystemPromptService systemPromptService;

    @GetMapping
    public AdminResponse<SystemPrompt> current() {
        SystemPrompt prompt = systemPromptService.getActivePrompt();
        return AdminResponse.ok(prompt);
    }

    @GetMapping("/history")
    public AdminResponse<List<SystemPrompt>> history() {
        List<SystemPrompt> history = systemPromptService.getHistory();
        return AdminResponse.ok(history);
    }

    @PutMapping
    public AdminResponse<Void> update(@Valid @RequestBody PromptRequest request) {
        systemPromptService.updatePrompt(request.getContent());
        return AdminResponse.ok();
    }

    @PutMapping("/rollback/{versionId}")
    public AdminResponse<Void> rollback(@PathVariable Long versionId) {
        systemPromptService.rollback(versionId);
        return AdminResponse.ok();
    }
}
