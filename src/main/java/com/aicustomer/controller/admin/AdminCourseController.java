package com.aicustomer.controller.admin;

import com.aicustomer.entity.Course;
import com.aicustomer.entity.CourseCategory;
import com.aicustomer.service.CourseCategoryService;
import com.aicustomer.service.CourseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/course")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;
    private final CourseCategoryService courseCategoryService;

    @GetMapping("/list")
    public AdminResponse<PageResult<Course>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        Page<Course> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Course::getName, name)
               .eq(categoryId != null, Course::getCategoryId, categoryId)
               .orderByDesc(Course::getCreateTime);
        Page<Course> result = courseService.page(pageParam, wrapper);
        return AdminResponse.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    public AdminResponse<Course> detail(@PathVariable Long id) {
        Course course = courseService.getById(id);
        if (course == null) {
            return AdminResponse.error(404, "课程不存在");
        }
        return AdminResponse.ok(course);
    }

    @PostMapping
    public AdminResponse<Void> create(@RequestBody Course course) {
        courseService.save(course);
        return AdminResponse.ok();
    }

    @PutMapping("/{id}")
    public AdminResponse<Void> update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        courseService.updateById(course);
        return AdminResponse.ok();
    }

    @DeleteMapping("/{id}")
    public AdminResponse<Void> delete(@PathVariable Long id) {
        courseService.removeById(id);
        return AdminResponse.ok();
    }
}
