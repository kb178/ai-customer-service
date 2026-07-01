package com.aicustomer.service;

import com.aicustomer.entity.CourseSchedule;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface CourseScheduleService extends IService<CourseSchedule> {
    List<CourseSchedule> getSchedulesByCampusCourseId(Long campusCourseId);
    List<CourseSchedule> getAvailableSchedules(Long campusId, Long courseId);
}
