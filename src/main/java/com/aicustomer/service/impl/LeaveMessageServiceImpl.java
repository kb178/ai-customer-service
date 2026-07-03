package com.aicustomer.service.impl;

import com.aicustomer.entity.LeaveMessage;
import com.aicustomer.mapper.LeaveMessageMapper;
import com.aicustomer.service.LeaveMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class LeaveMessageServiceImpl extends ServiceImpl<LeaveMessageMapper, LeaveMessage> implements LeaveMessageService {

    @Override
    public LeaveMessage createMessage(LeaveMessage message) {
        message.setStatus(0);
        save(message);
        return message;
    }
}
