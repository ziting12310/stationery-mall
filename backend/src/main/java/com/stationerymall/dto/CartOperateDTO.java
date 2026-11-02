package com.stationerymall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 购物车操作入参。
 * <ul>
 *   <li><b>加购</b>：{@code POST /api/cart/items}，productId 与 quantity 均必填；</li>
 *   <li><b>改数量</b>：{@code PUT /api/cart/items/{id}}，路径已指定购物车项，只需 quantity。</li>
 * </ul>
 */
@Data
public class CartOperateDTO {

    /** 商品 ID：加购时必填；修改数量时无需传（由路径参数定位购物车项） */
    @Positive(message = "商品 ID 必须为正整数")
    private Long productId;

    /** 数量：≥ 1，且不得超过商品当前库存（后者由 Service 层查库校验） */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;
}
