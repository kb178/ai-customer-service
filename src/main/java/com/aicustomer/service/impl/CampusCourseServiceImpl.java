package com.aicustomer.service.impl;

import com.aicustomer.entity.CampusCourse;
import com.aicustomer.mapper.CampusCourseMapper;
import com.aicustomer.service.CampusCourseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CampusCourseServiceImpl extends ServiceImpl<CampusCourseMapper, CampusCourse> implements CampusCourseService {

    @Override
    public List<CampusCourse> getCoursesByCampusId(Long campusId) {
        LambdaQueryWrapper<CampusCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampusCourse::getCampusId, campusId)
               .eq(CampusCourse::getStatus, 1);
        return list(wrapper);
    }

    @Override
    public List<CampusCourse> getCampusesByCourseId(Long courseId) {
        LambdaQueryWrapper<CampusCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampusCourse::getCourseId, courseId)
               .eq(CampusCourse::getStatus, 1);
        return list(wrapper);
    }

    @Override
    public CampusCourse getCampusCourse(Long campusId, Long courseId) {
        LambdaQueryWrapper<CampusCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampusCourse::getCampusId, campusId)
               .eq(CampusCourse::getCourseId, courseId);
        return getOne(wrapper);
    }
}
