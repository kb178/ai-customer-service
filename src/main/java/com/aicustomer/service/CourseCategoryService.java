package com.aicustomer.service;

import com.aicustomer.entity.CourseCategory;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface CourseCategoryService extends IService<CourseCategory> {
    List<CourseCategory> getAllCategories();
}
