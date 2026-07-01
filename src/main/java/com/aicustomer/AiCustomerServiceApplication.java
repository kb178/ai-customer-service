package com.aicustomer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI智能客服系统 - 主启动类
 * 
 * 功能说明：
 * - 基于Spring Boot 3构建的智能客服应用
 * - 集成Spring AI + DeepSeek大模型实现智能对话
 * - 使用MyBatis Plus操作MySQL数据库
 * - 支持课程推荐、预约试听、客户信息管理等功能
 * 
 * 启动方式：
 * - 运行 main 方法即可启动应用
 * - 默认端口：8080
 * - 访问地址：http://localhost:8080
 */
@SpringBootApplication
public class AiCustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCustomerServiceApplication.class, args);
    }
}
