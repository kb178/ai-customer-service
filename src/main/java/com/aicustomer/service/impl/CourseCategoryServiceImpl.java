package com.aicustomer.service.impl;

import com.aicustomer.entity.CourseCategory;
import com.aicustomer.mapper.CourseCategoryMapper;
import com.aicustomer.service.CourseCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {

    @Override
    public List<CourseCategory> getAllCategories() {
        LambdaQueryWrapper<CourseCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseCategory::getStatus, 1).orderByAsc(CourseCategory::getSortOrder);
        return list(wrapper);
    }
}
