package com.stationerymall.controller;

import com.stationerymall.Result;
import com.stationerymall.service.CategoryService;
import com.stationerymall.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口 {@code /api/categories}（公开访问，游客与普通用户均可）。
 * <p>
 * 严格对齐 api-spec：{@code /top} 一级分类、{@code /{parentId}/sub} 二级分类。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 一级分类列表：GET /api/categories/top */
    @GetMapping("/top")
    public Result<List<CategoryVO>> top() {
        return Result.success(categoryService.listLevelOne());
    }

    /** 指定一级分类下的二级分类：GET /api/categories/{parentId}/sub */
    @GetMapping("/{parentId}/sub")
    public Result<List<CategoryVO>> sub(@PathVariable Long parentId) {
        return Result.success(categoryService.listByParentId(parentId));
    }
}
