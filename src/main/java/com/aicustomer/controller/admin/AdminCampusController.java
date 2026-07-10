package com.aicustomer.controller.admin;

import com.aicustomer.entity.Campus;
import com.aicustomer.entity.CampusCourse;
import com.aicustomer.service.CampusCourseService;
import com.aicustomer.service.CampusService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/campus")
@RequiredArgsConstructor
public class AdminCampusController {

    private final CampusService campusService;
    private final CampusCourseService campusCourseService;

    @GetMapping("/list")
    public AdminResponse<PageResult<Campus>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long cityId) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        Page<Campus> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Campus> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Campus::getName, name)
               .eq(cityId != null, Campus::getCityId, cityId)
               .orderByDesc(Campus::getCreateTime);
        Page<Campus> result = campusService.page(pageParam, wrapper);
        return AdminResponse.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    public AdminResponse<Campus> detail(@PathVariable Long id) {
        Campus campus = campusService.getById(id);
        if (campus == null) {
            return AdminResponse.error(404, "校区不存在");
        }
        return AdminResponse.ok(campus);
    }

    @PostMapping
    public AdminResponse<Void> create(@RequestBody Campus campus) {
        campusService.save(campus);
        return AdminResponse.ok();
    }

    @PutMapping("/{id}")
    public AdminResponse<Void> update(@PathVariable Long id, @RequestBody Campus campus) {
        campus.setId(id);
        campusService.updateById(campus);
        return AdminResponse.ok();
    }

    @DeleteMapping("/{id}")
    public AdminResponse<Void> delete(@PathVariable Long id) {
        campusService.removeById(id);
        return AdminResponse.ok();
    }

    @GetMapping("/{id}/courses")
    public AdminResponse<List<CampusCourse>> campusCourses(@PathVariable Long id) {
        List<CampusCourse> list = campusCourseService.lambdaQuery()
                .eq(CampusCourse::getCampusId, id)
                .list();
        return AdminResponse.ok(list);
    }
}
