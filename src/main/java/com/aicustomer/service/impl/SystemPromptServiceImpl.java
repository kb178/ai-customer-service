package com.aicustomer.service.impl;

import com.aicustomer.entity.SystemPrompt;
import com.aicustomer.mapper.SystemPromptMapper;
import com.aicustomer.service.SystemPromptService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemPromptServiceImpl extends ServiceImpl<SystemPromptMapper, SystemPrompt> implements SystemPromptService {

    @Override
    public SystemPrompt getActivePrompt() {
        return lambdaQuery().eq(SystemPrompt::getIsActive, 1).one();
    }

    @Override
    public List<SystemPrompt> getHistory() {
        return lambdaQuery()
                .eq(SystemPrompt::getIsActive, 0)
                .orderByDesc(SystemPrompt::getVersion)
                .last("LIMIT 10")
                .list();
    }

    @Override
    @Transactional
    public void updatePrompt(String content) {
        // 获取当前最大版本号
        SystemPrompt current = getActivePrompt();
        int newVersion = (current != null ? current.getVersion() : 0) + 1;

        // 将当前生效的标记为历史版本
        if (current != null) {
            current.setIsActive(0);
            updateById(current);
        }

        // 创建新版本
        SystemPrompt prompt = new SystemPrompt();
        prompt.setContent(content);
        prompt.setVersion(newVersion);
        prompt.setIsActive(1);
        save(prompt);
    }

    @Override
    @Transactional
    public void rollback(Long id) {
        SystemPrompt target = getById(id);
        if (target == null) {
            throw new IllegalArgumentException("版本不存在");
        }

        // 将当前生效的标记为历史
        SystemPrompt current = getActivePrompt();
        if (current != null) {
            current.setIsActive(0);
            updateById(current);
        }

        // 获取最大版本号 + 1
        SystemPrompt latest = lambdaQuery().orderByDesc(SystemPrompt::getVersion).last("LIMIT 1").one();
        int newVersion = (latest != null ? latest.getVersion() : 0) + 1;

        // 创建回滚版本（内容来自目标版本，版本号递增）
        SystemPrompt rollback = new SystemPrompt();
        rollback.setContent(target.getContent());
        rollback.setVersion(newVersion);
        rollback.setIsActive(1);
        save(rollback);
    }
}
