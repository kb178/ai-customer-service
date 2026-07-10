package com.aicustomer.controller.admin;

import com.aicustomer.entity.Faq;
import com.aicustomer.service.FaqService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/faq")
@RequiredArgsConstructor
public class AdminFaqController {

    private final FaqService faqService;

    @GetMapping("/list")
    public AdminResponse<PageResult<Faq>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        Page<Faq> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Faq> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(question), Faq::getQuestion, question)
               .eq(StringUtils.hasText(category), Faq::getCategory, category)
               .eq(status != null, Faq::getStatus, status)
               .orderByAsc(Faq::getSortOrder)
               .orderByDesc(Faq::getCreateTime);
        Page<Faq> result = faqService.page(pageParam, wrapper);
        return AdminResponse.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    public AdminResponse<Faq> detail(@PathVariable Long id) {
        Faq faq = faqService.getById(id);
        if (faq == null) {
            return AdminResponse.error(404, "FAQ不存在");
        }
        return AdminResponse.ok(faq);
    }

    @PostMapping
    public AdminResponse<Void> create(@RequestBody Faq faq) {
        faqService.save(faq);
        return AdminResponse.ok();
    }

    @PutMapping("/{id}")
    public AdminResponse<Void> update(@PathVariable Long id, @RequestBody Faq faq) {
        faq.setId(id);
        faqService.updateById(faq);
        return AdminResponse.ok();
    }

    @DeleteMapping("/{id}")
    public AdminResponse<Void> delete(@PathVariable Long id) {
        faqService.removeById(id);
        return AdminResponse.ok();
    }
}
