package com.aicustomer.service.impl;

import com.aicustomer.entity.City;
import com.aicustomer.mapper.CityMapper;
import com.aicustomer.service.CityService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CityServiceImpl extends ServiceImpl<CityMapper, City> implements CityService {

    @Override
    public List<City> getCitiesByProvinceId(Long provinceId) {
        LambdaQueryWrapper<City> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(City::getProvinceId, provinceId)
               .eq(City::getStatus, 1)
               .orderByAsc(City::getSortOrder);
        return list(wrapper);
    }
}
