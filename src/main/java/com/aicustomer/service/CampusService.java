package com.aicustomer.service;

import com.aicustomer.entity.Campus;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 校区服务接口
 * 
 * 功能：定义校区相关的业务方法
 * 
 * 继承IService，已内置以下方法：
 * - save: 新增校区
 * - updateById: 更新校区
 * - removeById: 删除校区
 * - getById: 根据ID查询
 * - list: 查询列表
 * - page: 分页查询
 */
public interface CampusService extends IService<Campus> {

    /**
     * 获取所有启用的校区
     * 
     * 功能：查询所有状态为启用的校区列表
     * 
     * @return 启用状态的校区列表
     */
    List<Campus> getAllCampuses();
}
