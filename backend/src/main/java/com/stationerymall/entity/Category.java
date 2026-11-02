package com.stationerymall.entity;

import lombok.Data;

/**
 * 商品分类实体（对应表 {@code categories}），<b>两级自关联</b>结构。
 * <ul>
 *   <li>{@code parentId == null} → 一级分类（如「文具用品」）；</li>
 *   <li>{@code parentId != null} → 二级分类（如「圆珠笔」），<b>商品只能挂在二级分类下</b>（Service 层校验）。</li>
 * </ul>
 */
@Data
public class Category {

    /** 主键：categories.id */
    private Long id;

    /** 分类名称：categories.name，同层级内唯一 */
    private String name;

    /** 父分类 ID：categories.parent_id；NULL = 一级分类 */
    private Long parentId;

    /** 同级排序：categories.sort_order，数字越小越靠前 */
    private Integer sortOrder;
}
