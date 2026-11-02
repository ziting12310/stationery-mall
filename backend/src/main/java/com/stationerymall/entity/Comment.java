package com.stationerymall.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体（对应表 {@code evaluations}）。
 * <p>
 * 命名说明：物理表名沿用 database-design 的 {@code evaluations}，实体类按作业与接口语义命名 Comment。
 * <p>
 * 约束：{@code UNIQUE(order_id, product_id, user_id)} —— 同一订单 + 同一商品 + 同一用户只能评价一次；
 * 且仅当订单状态为 COMPLETED 时才允许提交（Service 层校验）。
 */
@Data
public class Comment {

    /** 主键：evaluations.id */
    private Long id;

    /** 订单 ID：evaluations.order_id */
    private Long orderId;

    /** 商品 ID：evaluations.product_id */
    private Long productId;

    /** 评价人：evaluations.user_id，必须等于订单所属用户 */
    private Long userId;

    /** 评价人用户名：<b>非表字段</b>，商品详情评价列表 LEFT JOIN users 回填 */
    private String username;

    /** 评分：evaluations.rating，1~5 星整数 */
    private Integer rating;

    /** 评论内容：evaluations.content，纯文字选填，≤ 500 字符，不支持图片 */
    private String content;

    /** 创建时间：evaluations.created_at */
    private LocalDateTime createdAt;
}
