package com.aicustomer.service;

import com.aicustomer.entity.CampusCourse;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface CampusCourseService extends IService<CampusCourse> {
    List<CampusCourse> getCoursesByCampusId(Long campusId);
    List<CampusCourse> getCampusesByCourseId(Long courseId);
    CampusCourse getCampusCourse(Long campusId, Long courseId);
}
