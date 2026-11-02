package com.stationerymall.mapper;

import com.stationerymall.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车表 {@code cart_items} 的纯数据访问接口。
 * <p>
 * 约束：不校验库存、上架状态、归属权（由 Service 层判断），只提供读写能力。
 * 唯一键 {@code (user_id, product_id)} 的「重复加购叠加数量」由上层先查后改实现。
 */
@Mapper
public interface CartItemMapper {

    /** 新增购物车项 */
    int insert(CartItem cartItem);

    /** 按主键查询 */
    CartItem selectById(@Param("id") Long id);

    /** 按「用户 + 商品」查询（同一组合最多一行） */
    CartItem selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 查询某用户的全部购物车项（按 id 升序） */
    List<CartItem> selectByUserId(@Param("userId") Long userId);

    /** 按 id 集合 + 用户查询（结算时校验勾选行归属） */
    List<CartItem> selectByIdsAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /** 更新数量 */
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 按主键删除 */
    int deleteById(@Param("id") Long id);

    /** 批量删除勾选行（带 user_id 条件，防止越权删他人购物车） */
    int deleteByIdsAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /** 清空某用户购物车 */
    int deleteByUserId(@Param("userId") Long userId);
}
