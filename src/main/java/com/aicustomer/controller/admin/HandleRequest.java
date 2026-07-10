package com.aicustomer.controller.admin;

import lombok.Data;

@Data
public class HandleRequest {
    /** 处理人 */
    private String handler;
    /** 处理备注 */
    private String handleRemark;
    /** 状态：1处理中 2已解决 3已忽略 */
    private Integer status;
}
