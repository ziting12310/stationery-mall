package com.stationerymall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价响应 VO（商品详情页评价列表 / 提交评价后返回）。
 */
@Data
public class CommentVO {

    /** 评价 ID */
    private Long id;

    /** 订单 ID */
    private Long orderId;

    /** 订单号（管理员评价列表展示用，由订单表关联回填） */
    private String orderNo;

    /** 商品 ID */
    private Long productId;

    /** 商品名称（商品详情页评价列表展示用） */
    private String productName;

    /** 评价人 ID */
    private Long userId;

    /** 评价人用户名（列表展示用） */
    private String username;

    /** 评分：1~5 星 */
    private Integer rating;

    /** 评论内容 */
    private String content;

    /** 评价时间 */
    private LocalDateTime createdAt;
}
