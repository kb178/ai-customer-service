package com.aicustomer.service.impl;

import com.aicustomer.entity.Province;
import com.aicustomer.mapper.ProvinceMapper;
import com.aicustomer.service.ProvinceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProvinceServiceImpl extends ServiceImpl<ProvinceMapper, Province> implements ProvinceService {

    @Override
    public List<Province> getAllProvinces() {
        LambdaQueryWrapper<Province> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Province::getStatus, 1).orderByAsc(Province::getSortOrder);
        return list(wrapper);
    }
}
