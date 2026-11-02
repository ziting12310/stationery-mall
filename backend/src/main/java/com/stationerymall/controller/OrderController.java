package com.stationerymall.controller;

import com.stationerymall.Result;
import com.stationerymall.dto.OrderCreateDTO;
import com.stationerymall.enums.OrderStatusEnum;
import com.stationerymall.service.OrderService;
import com.stationerymall.vo.OrderVO;
import com.stationerymall.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台订单接口 {@code /api/orders}（仅普通用户，管理员下单由 Service 返回 403）。
 * <p>
 * 状态机流转（付款 / 取消 / 确认收货）的合法性校验全部由 Service 基于 OrderStatusEnum 判断。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 创建订单：POST /api/orders（服务端重算金额，扣库存 + 写订单 + 写明细同事务） */
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    /**
     * 订单列表：GET /api/orders?status=&cursor=&size=（游标分页，status 按枚举名传入）。
     * <p>
     * 权限由 Service 依据 JWT 登录态判定：管理员看全部订单，普通用户仅看自己的订单；
     * 前端不传 userId，Controller 不参与权限判断。
     */
    @GetMapping
    public Result<PageVO<OrderVO>> list(@RequestParam(required = false) OrderStatusEnum status,
                                        @RequestParam(required = false) Long cursor,
                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.listOrdersByRole(status, cursor, size));
    }

    /** 订单详情：GET /api/orders/{id}（归属权校验在 Service） */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getMyOrderDetail(id));
    }

    /**
     * 新增付款接口：POST /api/orders/pay?orderId=xxx（UNPAID → PAID）。
     * <p>
     * Controller 只接收订单 id，订单是否存在、是否属于当前登录用户、状态是否待付款，
     * 全部由 Service 基于登录态与 OrderStatusEnum 校验。
     */
    @PostMapping("/pay")
    public Result<OrderVO> payByParam(@RequestParam Long orderId) {
        return Result.success(orderService.payOrder(orderId));
    }

    /**
     * 模拟付款（原接口保留，兼容 api-spec）：POST /api/orders/{id}/pay（UNPAID → PAID）。
     * 说明：api-spec 把模拟付款并入下单，但 PRD 状态机将其作为独立动作，此处按状态机保留独立接口。
     */
    @PostMapping("/{id}/pay")
    public Result<OrderVO> pay(@PathVariable Long id) {
        return Result.success(orderService.payOrder(id));
    }

    /** 取消订单：POST /api/orders/{id}/cancel（仅 UNPAID，取消回补库存） */
    @PostMapping("/{id}/cancel")
    public Result<OrderVO> cancel(@PathVariable Long id) {
        return Result.success(orderService.cancelOrder(id));
    }

    /** 确认收货：POST /api/orders/{id}/confirm-receipt（仅 SHIPPED → COMPLETED） */
    @PostMapping("/{id}/confirm-receipt")
    public Result<OrderVO> confirmReceipt(@PathVariable Long id) {
        return Result.success(orderService.confirmReceipt(id));
    }
}
