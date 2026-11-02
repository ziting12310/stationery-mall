package com.stationerymall.service;

import com.stationerymall.vo.CategoryVO;

import java.util.List;

/**
 * 分类服务（两级分类，只读）。
 * <p>
 * 分类由初始化 SQL 预置，MVP 不开放后台分类维护。
 */
public interface CategoryService {

    /** 两级分类树：一级分类 + children 二级分类（前台首页/导航使用） */
    List<CategoryVO> listTree();

    /** 所有一级分类 */
    List<CategoryVO> listLevelOne();

    /** 指定一级分类下的二级分类 */
    List<CategoryVO> listByParentId(Long parentId);
}
