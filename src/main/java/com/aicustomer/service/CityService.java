package com.aicustomer.service;

import com.aicustomer.entity.City;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface CityService extends IService<City> {
    List<City> getCitiesByProvinceId(Long provinceId);
}
