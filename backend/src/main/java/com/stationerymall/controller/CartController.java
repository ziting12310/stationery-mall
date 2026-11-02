package com.stationerymall.controller;

import com.stationerymall.Result;
import com.stationerymall.dto.CartOperateDTO;
import com.stationerymall.service.CartService;
import com.stationerymall.vo.CartSummaryVO;
import com.stationerymall.vo.CartVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车接口 {@code /api/cart}（仅普通用户）。
 * <p>
 * 角色与归属权校验全部在 Service 层完成（管理员 403、他人购物车 403）。
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** 购物车列表：GET /api/cart/items（下架商品由 Service 自动剔除，返回 items + totalAmount） */
    @GetMapping("/items")
    public Result<CartSummaryVO> items() {
        return Result.success(cartService.listMyCart());
    }

    /** 加入购物车：POST /api/cart/items（同商品重复加购只累加数量） */
    @PostMapping("/items")
    public Result<CartVO> add(@Valid @RequestBody CartOperateDTO dto) {
        return Result.success(cartService.add(dto));
    }

    /** 修改数量：PUT /api/cart/items/{id} */
    @PutMapping("/items/{id}")
    public Result<CartVO> updateQuantity(@PathVariable Long id,
                                         @Valid @RequestBody CartOperateDTO dto) {
        return Result.success(cartService.updateQuantity(id, dto));
    }

    /** 删除单项：DELETE /api/cart/items/{id} */
    @DeleteMapping("/items/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.removeItem(id);
        return Result.success();
    }

    /** 清空购物车：DELETE /api/cart */
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear();
        return Result.success();
    }
}
