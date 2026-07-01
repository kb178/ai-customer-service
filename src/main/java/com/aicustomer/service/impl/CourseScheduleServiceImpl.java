package com.aicustomer.service.impl;

import com.aicustomer.entity.CampusCourse;
import com.aicustomer.entity.CourseSchedule;
import com.aicustomer.mapper.CourseScheduleMapper;
import com.aicustomer.service.CampusCourseService;
import com.aicustomer.service.CourseScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseScheduleServiceImpl extends ServiceImpl<CourseScheduleMapper, CourseSchedule> implements CourseScheduleService {

    private final CampusCourseService campusCourseService;

    @Override
    public List<CourseSchedule> getSchedulesByCampusCourseId(Long campusCourseId) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedule::getCampusCourseId, campusCourseId)
               .eq(CourseSchedule::getStatus, 1)
               .orderByAsc(CourseSchedule::getDayOfWeek)
               .orderByAsc(CourseSchedule::getStartTime);
        return list(wrapper);
    }

    @Override
    public List<CourseSchedule> getAvailableSchedules(Long campusId, Long courseId) {
        // 获取校区课程关联
        CampusCourse campusCourse = campusCourseService.getCampusCourse(campusId, courseId);
        if (campusCourse == null) {
            return new ArrayList<>();
        }

        // 获取可用的时间段
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseSchedule::getCampusCourseId, campusCourse.getId())
               .eq(CourseSchedule::getStatus, 1);

        List<CourseSchedule> schedules = list(wrapper);

        // 过滤掉已满员的时间段
        return schedules.stream()
                .filter(s -> s.getMaxStudents() == null || s.getCurrentStudents() == null ||
                             s.getCurrentStudents() < s.getMaxStudents())
                .toList();
    }
}
