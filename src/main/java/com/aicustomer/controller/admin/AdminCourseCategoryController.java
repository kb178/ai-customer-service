package com.aicustomer.controller.admin;

import com.aicustomer.entity.CourseCategory;
import com.aicustomer.service.CourseCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/course-category")
@RequiredArgsConstructor
public class AdminCourseCategoryController {

    private final CourseCategoryService courseCategoryService;

    @GetMapping("/list")
    public AdminResponse<List<CourseCategory>> list() {
        List<CourseCategory> list = courseCategoryService.list();
        return AdminResponse.ok(list);
    }
}
