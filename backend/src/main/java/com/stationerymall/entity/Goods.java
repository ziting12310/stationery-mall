package com.stationerymall.entity;

import com.stationerymall.enums.GoodsStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（对应表 {@code products}）。
 * <p>
 * MVP 为单规格商品：一个商品一个价格一个库存，不做 SKU 多规格。
 * 实体类命名为 Goods（与前端/接口语义一致），物理表名仍为 {@code products}。
 * <p>
 * 注意：{@code onShelf} 在 DB 中是 {@code TINYINT(1)}，阶段 3 编写 Mapper 时需注册
 * TypeHandler 按 {@link GoodsStatusEnum#getCode()} 读写，只允许持久化的两个状态。
 */
@Data
public class Goods {

    /** 主键：products.id */
    private Long id;

    /** 商品名称：products.name；历史订单使用 order_items.product_name 快照，不随本字段变化 */
    private String name;

    /** 单价：products.price，DECIMAL(10,2)，禁止用 double 避免精度丢失 */
    private BigDecimal price;

    /** 库存：products.stock，INT UNSIGNED，必须 ≥ 0 */
    private Integer stock;

    /** 分类 ID：products.category_id，必须指向二级分类（Service 层校验 parent_id IS NOT NULL） */
    private Long categoryId;

    /** 商品描述：products.description，纯文本，前端展示需 XSS 清洗 */
    private String description;

    /** 图片 URL：products.image_url，MVP 单图，≤ 5 MB */
    private String imageUrl;

    /** 上架标志：products.on_shelf（1=上架 / 0=下架），仅取 ON_SHELF / OFF_SHELF 两个持久化状态 */
    private GoodsStatusEnum onShelf;

    /** 创建时间：products.created_at */
    private LocalDateTime createdAt;

    /** 更新时间：products.updated_at */
    private LocalDateTime updatedAt;
}
