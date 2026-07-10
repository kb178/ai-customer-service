package com.aicustomer.service;

import com.aicustomer.entity.SystemPrompt;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface SystemPromptService extends IService<SystemPrompt> {

    /** 获取当前生效的提示词 */
    SystemPrompt getActivePrompt();

    /** 获取最近10个历史版本 */
    List<SystemPrompt> getHistory();

    /** 更新提示词（创建新版本，旧版本标记为非生效） */
    void updatePrompt(String content);

    /** 回滚到指定版本 */
    void rollback(Long id);
}
