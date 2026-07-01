package com.aicustomer.service.impl;

import com.aicustomer.entity.Campus;
import com.aicustomer.mapper.CampusMapper;
import com.aicustomer.service.CampusService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 校区服务实现类
 * 
 * 功能：实现CampusService接口定义的校区相关业务方法
 */
@Service
public class CampusServiceImpl extends ServiceImpl<CampusMapper, Campus> implements CampusService {

    /**
     * 获取所有启用的校区
     * 
     * 实现逻辑：查询所有状态为启用（status=1）的校区
     * 
     * @return 启用状态的校区列表
     */
    @Override
    public List<Campus> getAllCampuses() {
        LambdaQueryWrapper<Campus> wrapper = new LambdaQueryWrapper<>();
        // 只查询启用状态的校区
        wrapper.eq(Campus::getStatus, 1);
        return list(wrapper);
    }
}
