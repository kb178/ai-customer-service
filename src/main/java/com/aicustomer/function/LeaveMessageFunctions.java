package com.aicustomer.function;

import com.aicustomer.entity.LeaveMessage;
import com.aicustomer.service.LeaveMessageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * 留言相关Function定义
 *
 * AI无法处理时（退款、投诉、特殊优惠等），记录学员留言
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LeaveMessageFunctions {

    private final LeaveMessageService leaveMessageService;

    /**
     * 记录学员留言
     */
    @Bean
    @Description("记录学员留言。当AI无法回答学员问题时，记录留言让客服跟进。参数：sessionId(会话ID)、customerName(姓名，可选)、customerPhone(电话，可选)、message(留言内容)、category(分类，如：退款咨询/课程问题/投诉建议)")
    public Function<LeaveMessageRequest, LeaveMessageResponse> leaveMessage() {
        return request -> {
            log.info("Function Calling - 记录留言: phone={}, category={}", request.getCustomerPhone(), request.getCategory());

            LeaveMessageResponse response = new LeaveMessageResponse();

            if (request.getMessage() == null || request.getMessage().isEmpty()) {
                response.setSuccess(false);
                response.setMessage("留言内容不能为空");
                return response;
            }

            LeaveMessage leaveMessage = new LeaveMessage();
            leaveMessage.setSessionId(request.getSessionId());
            leaveMessage.setCustomerName(request.getCustomerName());
            leaveMessage.setCustomerPhone(request.getCustomerPhone());
            leaveMessage.setMessage(request.getMessage());
            leaveMessage.setCategory(request.getCategory());

            leaveMessageService.createMessage(leaveMessage);

            response.setSuccess(true);
            response.setMessageId(leaveMessage.getId());
            response.setMessage("留言已记录，客服会在2小时内联系您");
            return response;
        };
    }

    @Data
    public static class LeaveMessageRequest {
        private String sessionId;
        private String customerName;
        private String customerPhone;
        private String message;
        private String category;
    }

    @Data
    public static class LeaveMessageResponse {
        private boolean success;
        private String message;
        private Long messageId;
    }
}
