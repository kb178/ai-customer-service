package com.aicustomer.controller.admin;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AdminLoginController {

    @PostMapping("/login")
    public AdminResponse<Void> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        if ("admin".equals(request.getUsername()) && "123456".equals(request.getPassword())) {
            session.setAttribute("adminUser", request.getUsername());
            return AdminResponse.ok();
        }
        return AdminResponse.error(401, "用户名或密码错误");
    }
}
