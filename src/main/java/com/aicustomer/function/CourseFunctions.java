package com.aicustomer.function;

import com.aicustomer.entity.Course;
import com.aicustomer.entity.CourseCategory;
import com.aicustomer.entity.CourseSchedule;
import com.aicustomer.entity.CampusCourse;
import com.aicustomer.service.CourseCategoryService;
import com.aicustomer.service.CourseScheduleService;
import com.aicustomer.service.CampusCourseService;
import com.aicustomer.service.CourseService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;

/**
 * 课程相关Function定义
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CourseFunctions {

    private final CourseService courseService;
    private final CourseCategoryService courseCategoryService;
    private final CampusCourseService campusCourseService;
    private final CourseScheduleService courseScheduleService;

    /**
     * 搜索课程函数
     */
    @Bean
    @Description("搜索课程信息。参数：keyword(关键词，可选)、categoryId(分类ID，可选)")
    public Function<SearchCoursesRequest, SearchCoursesResponse> searchCourses() {
        return request -> {
            log.info("Function Calling - 搜索课程: keyword={}, categoryId={}", request.getKeyword(), request.getCategoryId());
            List<Course> courses = courseService.searchCourses(request.getKeyword(), null);

            // 如果指定了分类ID，进一步过滤
            if (request.getCategoryId() != null) {
                courses = courses.stream()
                        .filter(c -> request.getCategoryId().equals(c.getCategoryId()))
                        .toList();
            }

            SearchCoursesResponse response = new SearchCoursesResponse();
            response.setCourses(courses);
            response.setTotal(courses.size());
            return response;
        };
    }

    /**
     * 获取所有课程分类
     */
    @Bean
    @Description("获取所有课程分类列表")
    public Function<GetCategoriesRequest, GetCategoriesResponse> getCategories() {
        return request -> {
            log.info("Function Calling - 获取课程分类");
            List<CourseCategory> categories = courseCategoryService.getAllCategories();
            GetCategoriesResponse response = new GetCategoriesResponse();
            response.setCategories(categories);
            response.setTotal(categories.size());
            return response;
        };
    }

    /**
     * 获取课程时间段
     */
    @Bean
    @Description("获取指定校区课程的时间安排。参数：campusId(校区ID)、courseId(课程ID)")
    public Function<GetCourseSchedulesRequest, GetCourseSchedulesResponse> getCourseSchedules() {
        return request -> {
            log.info("Function Calling - 获取课程时间: campusId={}, courseId={}", request.getCampusId(), request.getCourseId());

            CampusCourse campusCourse = campusCourseService.getCampusCourse(request.getCampusId(), request.getCourseId());
            if (campusCourse == null) {
                GetCourseSchedulesResponse response = new GetCourseSchedulesResponse();
                response.setFound(false);
                response.setMessage("该校区未开设此课程");
                return response;
            }

            List<CourseSchedule> schedules = courseScheduleService.getAvailableSchedules(request.getCampusId(), request.getCourseId());

            GetCourseSchedulesResponse response = new GetCourseSchedulesResponse();
            response.setFound(true);
            response.setCampusCourseId(campusCourse.getId());
            response.setMaxStudents(campusCourse.getMaxStudents());
            response.setCurrentStudents(campusCourse.getCurrentStudents());
            response.setSchedules(schedules);
            response.setTotal(schedules.size());
            return response;
        };
    }

    @Data
    public static class SearchCoursesRequest {
        private String keyword;
        private Long categoryId;
    }

    @Data
    public static class SearchCoursesResponse {
        private List<Course> courses;
        private int total;
    }

    @Data
    public static class GetCategoriesRequest {
    }

    @Data
    public static class GetCategoriesResponse {
        private List<CourseCategory> categories;
        private int total;
    }

    @Data
    public static class GetCourseSchedulesRequest {
        private Long campusId;
        private Long courseId;
    }

    @Data
    public static class GetCourseSchedulesResponse {
        private boolean found;
        private String message;
        private Long campusCourseId;
        private Integer maxStudents;
        private Integer currentStudents;
        private List<CourseSchedule> schedules;
        private int total;
    }
}
