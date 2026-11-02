package com.stationerymall.entity;

import lombok.Data;

/**
 * 购物车项实体（对应表 {@code cart_items}）。
 * <p>
 * 唯一约束 {@code UNIQUE(user_id, product_id)}：同一用户对同一商品只有一行，
 * 重复加购只<b>叠加数量</b>。
 * 购物车<b>不做价格快照</b>，展示时取商品当前价格。
 */
@Data
public class CartItem {

    /** 主键：cart_items.id */
    private Long id;

    /** 所属用户：cart_items.user_id，归属权校验基础 */
    private Long userId;

    /** 商品 ID：cart_items.product_id */
    private Long productId;

    /** 数量：cart_items.quantity，≥ 1，且不得超过 product.stock */
    private Integer quantity;
}
