package com.stationerymall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车整体响应 VO（对齐 api-spec 3.4.1：{@code {items, totalAmount}}）。
 * <p>
 * {@code totalAmount} 由服务端按当前商品价格实时累加，前端不要自行汇总。
 */
@Data
public class CartSummaryVO {

    /** 购物车行（已剔除下架商品） */
    private List<CartVO> items;

    /** 购物车合计金额（各条 subtotal 之和） */
    private BigDecimal totalAmount;
}
