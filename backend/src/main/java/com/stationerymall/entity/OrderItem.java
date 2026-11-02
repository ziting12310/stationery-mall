package com.stationerymall.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细实体（对应表 {@code order_items}）。
 * <p>
 * <b>价格快照</b>：{@code productName} 与 {@code unitPrice} 存的是<b>下单当时</b>的商品名称与单价，
 * 在下单事务内从 products 表 SELECT 后写入；之后商品改名/改价<b>绝不影响</b>历史订单。
 * 因此本实体<b>不</b>持有 Goods 关联对象，也<b>禁止</b>通过 JOIN 商品表实时计算金额。
 */
@Data
public class OrderItem {

    /** 主键：order_items.id */
    private Long id;

    /** 所属订单：order_items.order_id */
    private Long orderId;

    /** 商品 ID：order_items.product_id，仅用于数据完整性关联 */
    private Long productId;

    /** 商品名称（快照）：order_items.product_name —— 下单当时的名称 */
    private String productName;

    /** 单价（快照）：order_items.unit_price —— 下单当时的单价 */
    private BigDecimal unitPrice;

    /** 数量：order_items.quantity，≥ 1 */
    private Integer quantity;

    /** 小计：order_items.subtotal = unitPrice × quantity，事务内写入 */
    private BigDecimal subtotal;

    /**
     * 商品介绍（非 order_items 表字段）。
     * <p>
     * 由查询 SQL 关联 products 表取得（{@code p.description AS product_intro}），
     * MyBatis 驼峰映射 {@code product_intro → productIntro}，仅供订单列表 / 详情展示使用，插入时忽略。
     */
    private String productIntro;
}
