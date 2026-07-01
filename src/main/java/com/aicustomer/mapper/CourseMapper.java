package com.aicustomer.mapper;

import com.aicustomer.entity.Course;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程Mapper接口
 * 
 * 功能：提供课程数据的CRUD操作
 * 
 * 继承BaseMapper，已内置以下方法：
 * - insert: 新增课程
 * - deleteById: 根据ID删除课程
 * - updateById: 根据ID更新课程
 * - selectById: 根据ID查询课程
 * - selectList: 查询课程列表
 * - selectPage: 分页查询课程
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
