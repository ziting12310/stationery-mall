package com.stationerymall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评价提交请求入参。
 * <p>
 * 订单必须属于当前用户且状态为 COMPLETED、商品必须在该订单明细中、
 * 同一订单同一商品只能评价一次 —— 以上均由 Service 层校验（DTO 只做基础格式校验）。
 */
@Data
public class CommentSubmitDTO {

    /** 订单 ID */
    @NotNull(message = "订单 ID 不能为空")
    @Positive(message = "订单 ID 必须为正整数")
    private Long orderId;

    /** 被评价商品 ID */
    @NotNull(message = "商品 ID 不能为空")
    @Positive(message = "商品 ID 必须为正整数")
    private Long productId;

    /** 评分：1~5 星整数 */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer rating;

    /** 评论内容：纯文字选填，≤ 500 字符，不支持图片 */
    @Size(max = 500, message = "评论内容最多 500 字符")
    private String content;
}
