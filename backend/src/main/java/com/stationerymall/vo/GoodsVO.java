package com.stationerymall.vo;

import com.stationerymall.enums.GoodsStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品响应 VO（前台列表 / 详情 / 后台列表共用）。
 * <p>
 * {@code status} 为派生状态：上架且库存 > 0 → ON_SHELF；上架但库存 = 0 → ON_SALE_OUT_OF_STOCK；
 * 下架 → OFF_SHELF（由 {@link GoodsStatusEnum#resolve(Integer, int)} 推导）。
 */
@Data
public class GoodsVO {

    /** 商品 ID */
    private Long id;

    /** 商品名称 */
    private String name;

    /** 当前单价 */
    private BigDecimal price;

    /** 当前库存 */
    private Integer stock;

    /** 所属二级分类 ID */
    private Long categoryId;

    /** 所属二级分类名称（列表 / 详情展示用） */
    private String categoryName;

    /** 商品描述 */
    private String description;

    /** 商品图片 URL */
    private String imageUrl;

    /** 是否上架：true=上架（前台可见）/ false=下架（前台不可见） */
    private Boolean onShelf;

    /** 派生展示状态（含「上架-缺货」） */
    private GoodsStatusEnum status;

    /** 商品评价列表（仅详情场景填充，对齐 api-spec 3.3.3） */
    private List<CommentVO> evaluations;

    /** 评价总数（详情场景） */
    private Integer evaluationCount;

    /** 平均评分（详情场景，保留 1 位小数；无评价为 0.0） */
    private BigDecimal averageRating;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
