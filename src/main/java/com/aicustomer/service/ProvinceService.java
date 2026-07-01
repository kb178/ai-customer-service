package com.aicustomer.service;

import com.aicustomer.entity.Province;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ProvinceService extends IService<Province> {
    List<Province> getAllProvinces();
}
