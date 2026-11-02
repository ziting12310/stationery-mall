package com.stationerymall.service.impl;

import com.stationerymall.entity.Order;
import com.stationerymall.entity.OrderItem;
import com.stationerymall.enums.BizCodeEnum;
import com.stationerymall.exception.BusinessStateException;
import com.stationerymall.exception.ForbiddenException;
import com.stationerymall.exception.UnauthException;
import com.stationerymall.mapper.OrderItemMapper;
import com.stationerymall.mapper.OrderMapper;
import com.stationerymall.service.OrderItemService;
import com.stationerymall.util.UserContext;
import com.stationerymall.vo.OrderItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单明细服务实现（只读，含归属权校验）。
 */
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemMapper orderItemMapper;

    private final OrderMapper orderMapper;

    @Override
    public List<OrderItemVO> listByOrderId(Long orderId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new UnauthException();
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "订单不存在");
        }
        // 普通用户只能查看自己订单的明细，管理员可查全部
        if (!UserContext.isAdmin() && !userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权查看他人订单明细");
        }
        return orderItemMapper.selectByOrderId(orderId).stream().map(this::toVO).toList();
    }

    @Override
    public List<OrderItemVO> listByOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return orderItemMapper.selectByOrderIds(orderIds).stream().map(this::toVO).toList();
    }

    /** 明细实体转 VO（快照字段原样返回） */
    private OrderItemVO toVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setProductName(item.getProductName());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setQuantity(item.getQuantity());
        vo.setSubtotal(item.getSubtotal());
        return vo;
    }
}
