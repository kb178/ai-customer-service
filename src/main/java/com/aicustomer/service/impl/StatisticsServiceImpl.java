package com.aicustomer.service.impl;

import com.aicustomer.entity.Reservation;
import com.aicustomer.mapper.ReservationMapper;
import com.aicustomer.mapper.CustomerMapper;
import com.aicustomer.service.StatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final ReservationMapper reservationMapper;
    private final CustomerMapper customerMapper;

    @Override
    public Map<String, Object> getOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long todayCount = reservationMapper.selectCount(
            new LambdaQueryWrapper<Reservation>().ge(Reservation::getCreateTime, todayStart));
        long weekCount = reservationMapper.selectCount(
            new LambdaQueryWrapper<Reservation>().ge(Reservation::getCreateTime, weekStart));
        long monthCount = reservationMapper.selectCount(
            new LambdaQueryWrapper<Reservation>().ge(Reservation::getCreateTime, monthStart));
        long totalCustomers = customerMapper.selectCount(null);

        Map<String, Object> result = new HashMap<>();
        result.put("todayReservations", todayCount);
        result.put("weekReservations", weekCount);
        result.put("monthReservations", monthCount);
        result.put("totalCustomers", totalCustomers);
        return result;
    }

    @Override
    public List<Map<String, Object>> getReservationStatus() {
        List<Reservation> all = reservationMapper.selectList(null);
        Map<Integer, Long> grouped = all.stream()
            .collect(Collectors.groupingBy(Reservation::getStatus, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        String[] statusNames = {"待确认", "已确认", "已完成", "已取消"};
        for (int i = 0; i < statusNames.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("status", i);
            item.put("name", statusNames[i]);
            item.put("count", grouped.getOrDefault(i, 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTopCourses() {
        List<Reservation> all = reservationMapper.selectList(null);
        Map<Long, Long> grouped = all.stream()
            .filter(r -> r.getCourseId() != null)
            .collect(Collectors.groupingBy(Reservation::getCourseId, Collectors.counting()));

        return grouped.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("courseId", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTopCampuses() {
        List<Reservation> all = reservationMapper.selectList(null);
        Map<Long, Long> grouped = all.stream()
            .filter(r -> r.getCampusId() != null)
            .collect(Collectors.groupingBy(Reservation::getCampusId, Collectors.counting()));

        return grouped.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("campusId", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getConversion() {
        long totalCustomers = customerMapper.selectCount(null);
        long totalReservations = reservationMapper.selectCount(null);
        double rate = totalCustomers > 0 ? (double) totalReservations / totalCustomers * 100 : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalCustomers", totalCustomers);
        result.put("totalReservations", totalReservations);
        result.put("conversionRate", Math.round(rate * 100.0) / 100.0);
        return result;
    }
}
