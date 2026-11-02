package com.stationerymall.entity;

import com.stationerymall.enums.OrderStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主单实体（对应表 {@code orders}）。
 * <p>
 * 一次勾选结算 = 一张订单；状态严格 5 态，流转能力由 {@link OrderStatusEnum} 提供，
 * 禁止在本实体上写死数字。
 * <p>
 * 注意：{@code status} 在 DB 中是 {@code TINYINT}，阶段 3 编写 Mapper 时需注册
 * TypeHandler 按 {@link OrderStatusEnum#getCode()} 读写。
 */
@Data
public class Order {

    /** 主键：orders.id */
    private Long id;

    /** 业务订单号：orders.order_no，唯一，对用户展示（如 SO20261029143052001） */
    private String orderNo;

    /** 所属用户：orders.user_id，归属权校验基础 */
    private Long userId;

    /** 下单用户名：<b>非表字段</b>，后台订单列表 / 详情 LEFT JOIN users 回填 */
    private String username;

    /** 订单状态：orders.status，0=UNPAID / 1=PAID / 2=SHIPPED / 3=COMPLETED / 4=CANCELLED */
    private OrderStatusEnum status;

    /** 订单总金额：orders.total_amount，<b>服务端重算</b>，前端传值不采信 */
    private BigDecimal totalAmount;

    /** 收货地址：orders.address，MVP 纯文本框，不做地址簿 */
    private String address;

    /** 下单备注：orders.remark，选填 */
    private String remark;

    /** 创建时间：orders.created_at，下单事务内写入 */
    private LocalDateTime createdAt;

    /** 付款时间：orders.paid_at，模拟付款时写入 */
    private LocalDateTime paidAt;

    /** 发货时间：orders.shipped_at，管理员标记发货时写入 */
    private LocalDateTime shippedAt;

    /** 完成时间：orders.completed_at，用户确认收货时写入 */
    private LocalDateTime completedAt;

    /** 取消时间：orders.cancelled_at，用户取消时写入 */
    private LocalDateTime cancelledAt;
}
