package com.aicustomer.service;

import com.aicustomer.entity.Course;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 课程服务接口
 * 
 * 功能：定义课程相关的业务方法
 * 
 * 继承IService，已内置以下方法：
 * - save: 新增课程
 * - updateById: 更新课程
 * - removeById: 删除课程
 * - getById: 根据ID查询
 * - list: 查询列表
 * - page: 分页查询
 */
public interface CourseService extends IService<Course> {

    /**
     * 搜索课程
     * 
     * 功能：根据关键词和分类搜索课程
     * 
     * @param keyword 搜索关键词（模糊匹配课程名称和描述）
     * @param category 课程分类（精确匹配）
     * @return 符合条件的课程列表
     */
    List<Course> searchCourses(String keyword, String category);
}
