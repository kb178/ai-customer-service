package com.aicustomer.function;

import com.aicustomer.entity.Campus;
import com.aicustomer.entity.CampusCourse;
import com.aicustomer.entity.City;
import com.aicustomer.entity.Province;
import com.aicustomer.service.CampusCourseService;
import com.aicustomer.service.CampusService;
import com.aicustomer.service.CityService;
import com.aicustomer.service.ProvinceService;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 校区/地区相关Function定义
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CampusFunctions {

    private final CampusService campusService;
    private final ProvinceService provinceService;
    private final CityService cityService;
    private final CampusCourseService campusCourseService;

    /**
     * 获取有校区的省份（只返回开设了课程的省份）
     */
    @Bean
    @Description("获取有校区的省份列表")
    public Function<GetProvincesRequest, GetProvincesResponse> getProvinces() {
        return request -> {
            log.info("Function Calling - 获取有校区的省份列表");
            // 获取所有有校区的省份ID
            List<Campus> campuses = campusService.getAllCampuses();
            List<Long> provinceIds = campuses.stream()
                    .map(Campus::getProvinceId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();

            if (provinceIds.isEmpty()) {
                GetProvincesResponse response = new GetProvincesResponse();
                response.setProvinces(List.of());
                response.setTotal(0);
                return response;
            }

            // 查询这些省份
            List<Province> provinces = provinceService.listByIds(provinceIds);

            GetProvincesResponse response = new GetProvincesResponse();
            response.setProvinces(provinces);
            response.setTotal(provinces.size());
            return response;
        };
    }

    /**
     * 获取省份下有校区的城市
     */
    @Bean
    @Description("获取指定省份下有校区的城市列表。参数：provinceId(省份ID)")
    public Function<GetCitiesRequest, GetCitiesResponse> getCities() {
        return request -> {
            log.info("Function Calling - 获取有校区的城市: provinceId={}", request.getProvinceId());
            // 获取该省份下有校区的城市ID
            List<Campus> campuses = campusService.lambdaQuery()
                    .eq(Campus::getProvinceId, request.getProvinceId())
                    .eq(Campus::getStatus, 1)
                    .list();
            List<Long> cityIds = campuses.stream()
                    .map(Campus::getCityId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();

            if (cityIds.isEmpty()) {
                GetCitiesResponse response = new GetCitiesResponse();
                response.setCities(List.of());
                response.setTotal(0);
                return response;
            }

            List<City> cities = cityService.listByIds(cityIds);

            GetCitiesResponse response = new GetCitiesResponse();
            response.setCities(cities);
            response.setTotal(cities.size());
            return response;
        };
    }

    /**
     * 获取校区列表（支持按省份/城市/课程ID筛选）
     */
    @Bean
    @Description("获取校区列表。参数：provinceId(省份ID，可选)、cityId(城市ID，可选)、courseId(课程ID，可选，筛选开设该课程的校区)")
    public Function<GetCampusesRequest, GetCampusesResponse> getCampuses() {
        return request -> {
            log.info("Function Calling - 获取校区: provinceId={}, cityId={}, courseId={}",
                    request.getProvinceId(), request.getCityId(), request.getCourseId());

            List<Campus> campuses;

            if (request.getCourseId() != null) {
                // 先找开设该课程的校区ID
                List<CampusCourse> campusCourses = campusCourseService.getCampusesByCourseId(request.getCourseId());
                List<Long> campusIds = campusCourses.stream()
                        .map(CampusCourse::getCampusId)
                        .toList();

                if (campusIds.isEmpty()) {
                    GetCampusesResponse response = new GetCampusesResponse();
                    response.setCampuses(List.of());
                    response.setTotal(0);
                    return response;
                }

                var query = campusService.lambdaQuery()
                        .in(Campus::getId, campusIds)
                        .eq(Campus::getStatus, 1);

                if (request.getCityId() != null) {
                    query.eq(Campus::getCityId, request.getCityId());
                } else if (request.getProvinceId() != null) {
                    query.eq(Campus::getProvinceId, request.getProvinceId());
                }

                campuses = query.list();
            } else if (request.getCityId() != null) {
                campuses = campusService.lambdaQuery()
                        .eq(Campus::getCityId, request.getCityId())
                        .eq(Campus::getStatus, 1)
                        .list();
            } else if (request.getProvinceId() != null) {
                campuses = campusService.lambdaQuery()
                        .eq(Campus::getProvinceId, request.getProvinceId())
                        .eq(Campus::getStatus, 1)
                        .list();
            } else {
                campuses = campusService.getAllCampuses();
            }

            GetCampusesResponse response = new GetCampusesResponse();
            response.setCampuses(campuses);
            response.setTotal(campuses.size());
            return response;
        };
    }

    /**
     * 获取校区开设的课程
     */
    @Bean
    @Description("获取指定校区开设的课程列表。参数：campusId(校区ID)")
    public Function<GetCampusCoursesRequest, GetCampusCoursesResponse> getCampusCourses() {
        return request -> {
            log.info("Function Calling - 获取校区课程: campusId={}", request.getCampusId());
            List<CampusCourse> campusCourses = campusCourseService.getCoursesByCampusId(request.getCampusId());
            GetCampusCoursesResponse response = new GetCampusCoursesResponse();
            response.setCampusCourses(campusCourses);
            response.setTotal(campusCourses.size());
            return response;
        };
    }

    @Data
    public static class GetProvincesRequest {
    }

    @Data
    public static class GetProvincesResponse {
        private List<Province> provinces;
        private int total;
    }

    @Data
    public static class GetCitiesRequest {
        private Long provinceId;
        public void setProvinceId(Object value) { this.provinceId = LongParser.parse(value); }
    }

    @Data
    public static class GetCitiesResponse {
        private List<City> cities;
        private int total;
    }

    @Data
    public static class GetCampusesRequest {
        private Long provinceId;
        private Long cityId;
        private Long courseId;

        public void setProvinceId(Object value) { this.provinceId = LongParser.parse(value); }
        public void setCityId(Object value) { this.cityId = LongParser.parse(value); }
        public void setCourseId(Object value) { this.courseId = LongParser.parse(value); }
    }

    @Data
    public static class GetCampusesResponse {
        private List<Campus> campuses;
        private int total;
    }

    @Data
    public static class GetCampusCoursesRequest {
        private Long campusId;
        public void setCampusId(Object value) { this.campusId = LongParser.parse(value); }
    }

    @Data
    public static class GetCampusCoursesResponse {
        private List<CampusCourse> campusCourses;
        private int total;
    }
}
