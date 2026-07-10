package com.aicustomer.service;

import java.util.List;
import java.util.Map;

public interface StatisticsService {

    /** 概览数据：今日/本周/本月预约数 */
    Map<String, Object> getOverview();

    /** 预约状态分布 */
    List<Map<String, Object>> getReservationStatus();

    /** 热门课程 TOP5 */
    List<Map<String, Object>> getTopCourses();

    /** 热门校区 TOP5 */
    List<Map<String, Object>> getTopCampuses();

    /** 预约转化率（咨询→预约） */
    Map<String, Object> getConversion();
}
