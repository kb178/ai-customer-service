package com.aicustomer.controller.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> AdminResponse<T> ok(T data) {
        return new AdminResponse<>(200, "success", data);
    }

    public static <T> AdminResponse<T> ok() {
        return new AdminResponse<>(200, "success", null);
    }

    public static <T> AdminResponse<T> error(int code, String message) {
        return new AdminResponse<>(code, message, null);
    }
}
