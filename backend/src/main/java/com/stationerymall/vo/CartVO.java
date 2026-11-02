package com.stationerymall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项响应 VO（一条购物车行）。
 * <p>
 * 购物车不做价格快照，{@code price} / {@code productName} 等均为商品<b>当前</b>信息；
 * {@code subtotal = price × quantity} 由服务端计算。
 */
@Data
public class CartVO {

    /** 购物车项 ID（cart_items.id），前端改数量 / 删除时使用 */
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 商品名称（当前值） */
    private String productName;

    /** 商品图片 URL */
    private String imageUrl;

    /** 商品当前单价 */
    private BigDecimal price;

    /** 加购数量 */
    private Integer quantity;

    /** 商品当前库存，用于前端数量上限提示 */
    private Integer stock;

    /** 商品是否上架；false 时前端标灰并提示「已下架」，结算时自动剔除 */
    private Boolean onShelf;

    /** 是否可结算：已上架且库存 ≥ 数量 */
    private Boolean available;

    /** 小计：price × quantity */
    private BigDecimal subtotal;
}
