package com.stationerymall.vo;

import lombok.Data;

import java.util.List;

/**
 * 分类响应 VO（两级结构）。
 * <p>
 * 返回一级分类列表时填充 {@code children}；返回二级分类时 {@code children} 为空。
 */
@Data
public class CategoryVO {

    /** 分类 ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 父分类 ID；null 表示一级分类 */
    private Long parentId;

    /** 排序序号，越小越靠前 */
    private Integer sortOrder;

    /** 子分类（仅一级分类挂载时填充） */
    private List<CategoryVO> children;
}
