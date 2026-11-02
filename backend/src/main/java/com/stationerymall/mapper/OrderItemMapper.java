package com.stationerymall.mapper;

import com.stationerymall.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细表 {@code order_items} 的纯数据访问接口。
 * <p>
 * 约束：明细中的 {@code productName} / {@code unitPrice} 是<b>下单快照</b>，
 * 由 Service 在事务内从商品表读取后写入，<b>不与 goods 表实时联动</b>。
 */
@Mapper
public interface OrderItemMapper {

    /** 批量插入订单明细（一次结算的多行明细） */
    int batchInsert(@Param("items") List<OrderItem> items);

    /** 查询单张订单的全部明细（订单详情页） */
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /** 按订单 id 集合批量查询明细（列表页避免 N+1 查询） */
    List<OrderItem> selectByOrderIds(@Param("orderIds") List<Long> orderIds);
}
