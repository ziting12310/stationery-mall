package com.stationerymall.service;

import com.stationerymall.dto.OrderCreateDTO;
import com.stationerymall.enums.OrderStatusEnum;
import com.stationerymall.vo.OrderVO;
import com.stationerymall.vo.PageVO;

/**
 * 订单服务：下单、模拟付款、取消、确认收货、后台发货、列表与详情。
 * <p>
 * 状态机（作业 5 态）：
 * <pre>
 * UNPAID(待支付) ──付款──► PAID(已支付) ──管理员发货──► SHIPPED(已发货) ──确认收货──► COMPLETED(已完成，终态)
 *      └──取消（库存回补）──► CANCELLED(已取消，终态)
 * </pre>
 * 说明：Prompt 中的 PENDING_PAY 即本系统的 UNPAID。
 */
public interface OrderService {

    /** 下单（事务）：校验购物车归属 + 商品上架 + 库存，服务端重算总价，扣库存 → 写订单 → 写明细 → 清购物车 */
    OrderVO createOrder(OrderCreateDTO dto);

    /** 模拟付款：UNPAID → PAID（仅订单所属用户） */
    OrderVO payOrder(Long orderId);

    /** 取消订单：仅 UNPAID，取消后库存回补（仅订单所属用户） */
    OrderVO cancelOrder(Long orderId);

    /** 确认收货：SHIPPED → COMPLETED（仅订单所属用户） */
    OrderVO confirmReceipt(Long orderId);

    /** 管理员标记发货：PAID → SHIPPED（仅管理员） */
    OrderVO adminShip(Long orderId);

    /** 我的订单列表（可按状态筛选，游标分页） */
    PageVO<OrderVO> listMyOrders(OrderStatusEnum status, Long cursor, Integer size);

    /**
     * 订单列表（按登录角色自动收敛数据范围，游标分页）。
     * <pre>
     * ADMIN      → 返回全部用户订单
     * NORMAL_USER→ 仅返回 user_id = 当前登录用户 id 的订单
     * </pre>
     * 登录用户 id 与角色均取自 JWT 登录态（UserContext），前端不传递 userId。
     */
    PageVO<OrderVO> listOrdersByRole(OrderStatusEnum status, Long cursor, Integer size);

    /** 我的订单详情（含明细）；管理员也可查看 */
    OrderVO getMyOrderDetail(Long orderId);

    /** 后台全部订单列表（仅管理员，可按状态筛选，游标分页） */
    PageVO<OrderVO> adminListOrders(OrderStatusEnum status, Long cursor, Integer size);
}
