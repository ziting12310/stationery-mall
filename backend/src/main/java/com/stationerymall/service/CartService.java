package com.stationerymall.service;

import com.stationerymall.dto.CartOperateDTO;
import com.stationerymall.vo.CartSummaryVO;
import com.stationerymall.vo.CartVO;

/**
 * 购物车服务（仅普通用户；管理员禁止加购）。
 */
public interface CartService {

    /** 加购：同一商品重复加购只累加数量，数量不得超过库存 */
    CartVO add(CartOperateDTO dto);

    /** 我的购物车（items + 服务端合计金额）；下架商品会被自动剔除并跳过返回 */
    CartSummaryVO listMyCart();

    /** 修改购物车项数量（归属权校验） */
    CartVO updateQuantity(Long cartItemId, CartOperateDTO dto);

    /** 删除购物车项（归属权校验） */
    void removeItem(Long cartItemId);

    /** 清空我的购物车 */
    void clear();
}
