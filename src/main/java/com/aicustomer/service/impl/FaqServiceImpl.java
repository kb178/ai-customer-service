package com.aicustomer.service.impl;

import com.aicustomer.entity.Faq;
import com.aicustomer.mapper.FaqMapper;
import com.aicustomer.service.FaqService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FaqServiceImpl extends ServiceImpl<FaqMapper, Faq> implements FaqService {
}
