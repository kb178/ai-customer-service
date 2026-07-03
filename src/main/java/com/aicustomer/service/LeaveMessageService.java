package com.aicustomer.service;

import com.aicustomer.entity.LeaveMessage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 留言服务接口
 */
public interface LeaveMessageService extends IService<LeaveMessage> {

    /**
     * 创建留言
     */
    LeaveMessage createMessage(LeaveMessage message);
}
