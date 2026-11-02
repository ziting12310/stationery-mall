package com.stationerymall.service.impl;

import com.stationerymall.dto.CartOperateDTO;
import com.stationerymall.entity.CartItem;
import com.stationerymall.entity.Goods;
import com.stationerymall.enums.BizCodeEnum;
import com.stationerymall.enums.GoodsStatusEnum;
import com.stationerymall.exception.BusinessStateException;
import com.stationerymall.exception.ForbiddenException;
import com.stationerymall.exception.StockNotEnoughException;
import com.stationerymall.exception.UnauthException;
import com.stationerymall.mapper.CartItemMapper;
import com.stationerymall.mapper.GoodsMapper;
import com.stationerymall.service.CartService;
import com.stationerymall.util.UserContext;
import com.stationerymall.vo.CartSummaryVO;
import com.stationerymall.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务实现（仅普通用户）。
 * <p>
 * 规则：同一商品重复加购只累加数量；数量不得超过库存；下架商品不能加购，
 * 购物车列表会将已下架的行自动删除后再返回。
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;

    private final GoodsMapper goodsMapper;

    @Override
    public CartVO add(CartOperateDTO dto) {
        Long userId = requireNormalUser();

        Goods goods = goodsMapper.selectById(dto.getProductId());
        if (goods == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "商品不存在");
        }
        if (goods.getOnShelf() != GoodsStatusEnum.ON_SHELF) {
            throw new BusinessStateException("该商品已下架，无法加入购物车");
        }

        // 同一商品已存在 → 数量叠加；否则新增
        CartItem exists = cartItemMapper.selectByUserIdAndProductId(userId, dto.getProductId());
        int newQuantity = dto.getQuantity() + (exists == null ? 0 : exists.getQuantity());
        if (goods.getStock() < newQuantity) {
            throw new StockNotEnoughException(goods.getName(), goods.getStock());
        }

        if (exists == null) {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(dto.getProductId());
            item.setQuantity(newQuantity);
            cartItemMapper.insert(item);
            return toVO(item, goods);
        }

        cartItemMapper.updateQuantity(exists.getId(), newQuantity);
        exists.setQuantity(newQuantity);
        return toVO(exists, goods);
    }

    @Override
    public CartSummaryVO listMyCart() {
        Long userId = requireNormalUser();
        List<CartItem> items = cartItemMapper.selectByUserId(userId);

        List<CartVO> result = new ArrayList<>(items.size());
        List<Long> offShelfIds = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : items) {
            Goods goods = goodsMapper.selectById(item.getProductId());
            if (goods == null || goods.getOnShelf() != GoodsStatusEnum.ON_SHELF) {
                // 商品已下架 / 已删除：结算前自动剔除（物理删除，无需事务）
                offShelfIds.add(item.getId());
                continue;
            }
            CartVO vo = toVO(item, goods);
            result.add(vo);
            // 合计金额由服务端实时累加，不采信前端 total
            totalAmount = totalAmount.add(vo.getSubtotal());
        }
        if (!offShelfIds.isEmpty()) {
            cartItemMapper.deleteByIdsAndUserId(offShelfIds, userId);
        }

        CartSummaryVO summary = new CartSummaryVO();
        summary.setItems(result);
        summary.setTotalAmount(totalAmount);
        return summary;
    }

    @Override
    public CartVO updateQuantity(Long cartItemId, CartOperateDTO dto) {
        Long userId = requireNormalUser();
        CartItem item = getOwnedItem(cartItemId, userId);

        Goods goods = goodsMapper.selectById(item.getProductId());
        if (goods == null || goods.getOnShelf() != GoodsStatusEnum.ON_SHELF) {
            throw new BusinessStateException("该商品已下架，无法修改数量");
        }
        if (goods.getStock() < dto.getQuantity()) {
            throw new StockNotEnoughException(goods.getName(), goods.getStock());
        }

        cartItemMapper.updateQuantity(cartItemId, dto.getQuantity());
        item.setQuantity(dto.getQuantity());
        return toVO(item, goods);
    }

    @Override
    public void removeItem(Long cartItemId) {
        Long userId = requireNormalUser();
        CartItem item = getOwnedItem(cartItemId, userId);
        cartItemMapper.deleteById(item.getId());
    }

    @Override
    public void clear() {
        Long userId = requireNormalUser();
        cartItemMapper.deleteByUserId(userId);
    }

    /* ==================== 私有辅助 ==================== */

    /**
     * 取当前登录的普通用户 id：
     * 未登录 → 401；管理员 → 403（管理员禁止任何购物行为）。
     */
    private Long requireNormalUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new UnauthException();
        }
        if (!UserContext.getRole().canPlaceOrder()) {
            throw new ForbiddenException("管理员不能进行购物相关操作");
        }
        return userId;
    }

    /** 查询购物车项并校验归属权，越权直接 403 */
    private CartItem getOwnedItem(Long cartItemId, Long userId) {
        CartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "购物车项不存在");
        }
        if (!userId.equals(item.getUserId())) {
            throw new ForbiddenException("无权操作他人的购物车");
        }
        return item;
    }

    /** 购物车项 + 商品实时信息 → VO（购物车不做价格快照） */
    private CartVO toVO(CartItem item, Goods goods) {
        CartVO vo = new CartVO();
        vo.setId(item.getId());
        vo.setProductId(goods.getId());
        vo.setProductName(goods.getName());
        vo.setImageUrl(goods.getImageUrl());
        vo.setPrice(goods.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setStock(goods.getStock());
        vo.setOnShelf(true);
        vo.setAvailable(goods.getStock() >= item.getQuantity());
        vo.setSubtotal(goods.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return vo;
    }
}
