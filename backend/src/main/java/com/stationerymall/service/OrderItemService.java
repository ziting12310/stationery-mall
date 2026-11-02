package com.stationerymall.service;

import com.stationerymall.vo.OrderItemVO;

import java.util.List;

/**
 * 订单明细服务（只读）。
 * <p>
 * 明细中的商品名与单价均为<b>下单快照</b>，不随商品表变化。
 */
public interface OrderItemService {

    /** 查询某订单的明细（校验订单归属；管理员可查全部） */
    List<OrderItemVO> listByOrderId(Long orderId);

    /** 按订单 id 集合批量查询明细（订单列表页汇总使用） */
    List<OrderItemVO> listByOrderIds(List<Long> orderIds);
}
