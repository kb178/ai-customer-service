package com.aicustomer.controller.admin;

import com.aicustomer.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public AdminResponse<Map<String, Object>> overview() {
        return AdminResponse.ok(statisticsService.getOverview());
    }

    @GetMapping("/reservation-status")
    public AdminResponse<List<Map<String, Object>>> reservationStatus() {
        return AdminResponse.ok(statisticsService.getReservationStatus());
    }

    @GetMapping("/top-courses")
    public AdminResponse<List<Map<String, Object>>> topCourses() {
        return AdminResponse.ok(statisticsService.getTopCourses());
    }

    @GetMapping("/top-campuses")
    public AdminResponse<List<Map<String, Object>>> topCampuses() {
        return AdminResponse.ok(statisticsService.getTopCampuses());
    }

    @GetMapping("/conversion")
    public AdminResponse<Map<String, Object>> conversion() {
        return AdminResponse.ok(statisticsService.getConversion());
    }
}
