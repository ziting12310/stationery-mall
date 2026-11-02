package com.stationerymall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细响应 VO。
 * <p>
 * {@code productName} 与 {@code unitPrice} 来自 order_items 的<b>下单快照</b>，
 * 不随商品当前信息变化。
 */
@Data
public class OrderItemVO {

    /** 明细 ID */
    private Long id;

    /** 商品 ID（仅用于跳转商品详情，展示一律用快照字段） */
    private Long productId;

    /** 商品名称（下单时快照） */
    private String productName;

    /** 单价（下单时快照） */
    private BigDecimal unitPrice;

    /** 数量 */
    private Integer quantity;

    /** 小计 = unitPrice × quantity */
    private BigDecimal subtotal;

    /** 商品介绍（由 OrderItem.productIntro 转换而来，来自商品表 description，可能为 null） */
    private String productIntro;

    /** 该商品是否已评价（由服务端按评价表回填，前端据此切换评价区状态） */
    private Boolean evaluated;

    /** 已评价时的评价文字（未评价为 null，前端只读展示） */
    private String evaluationContent;
}
