package com.aicustomer.service.impl;

import com.aicustomer.entity.Course;
import com.aicustomer.mapper.CourseMapper;
import com.aicustomer.service.CourseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 课程服务实现类
 * 
 * 功能：实现CourseService接口定义的课程相关业务方法
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    /**
     * 搜索课程
     * 
     * 实现逻辑：
     * 1. 默认只查询启用状态的课程
     * 2. 如果有关键词，模糊匹配课程名称或描述
     * 3. 如果有分类，精确匹配课程分类
     * 
     * @param keyword 搜索关键词
     * @param category 课程分类
     * @return 符合条件的课程列表
     */
    @Override
    public List<Course> searchCourses(String keyword, String category) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        // 只查询启用状态的课程
        wrapper.eq(Course::getStatus, 1);
        
        // 关键词模糊搜索（匹配名称或描述）
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Course::getName, keyword)
                    .or().like(Course::getDescription, keyword));
        }
        
        // 分类精确匹配
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Course::getCategory, category);
        }
        
        return list(wrapper);
    }
}
