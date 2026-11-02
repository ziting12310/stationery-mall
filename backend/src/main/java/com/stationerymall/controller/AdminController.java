package com.stationerymall.controller;

import com.stationerymall.Result;
import com.stationerymall.dto.GoodsSaveDTO;
import com.stationerymall.dto.GoodsUpdateDTO;
import com.stationerymall.enums.OrderStatusEnum;
import com.stationerymall.service.GoodsService;
import com.stationerymall.service.OrderService;
import com.stationerymall.vo.GoodsVO;
import com.stationerymall.vo.OrderVO;
import com.stationerymall.vo.PageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.DeleteMapping;


/**
 * 后台管理接口 {@code /api/admin}（仅管理员）。
 * <p>
 * 角色校验在 Service 层二次兜底：即使绕过前端入口，非管理员也会得到 403。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final GoodsService goodsService;

    private final OrderService orderService;

    /** 商品列表（含已下架）：GET /api/admin/products?categoryId=&onShelf=&cursor=&size=（游标分页） */
    @GetMapping("/products")
    public Result<PageVO<GoodsVO>> products(@RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Boolean onShelf,
                                            @RequestParam(required = false) Long cursor,
                                            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(goodsService.adminList(categoryId, onShelf, cursor, size));
    }

    /** 新增商品：POST /api/admin/products */
    @PostMapping("/products")
    public Result<GoodsVO> createProduct(@Valid @RequestBody GoodsSaveDTO dto) {
        return Result.success(goodsService.adminCreate(dto));
    }

    /** 编辑商品：PUT /api/admin/products/{id}（所有字段可选，传了才更新） */
    @PutMapping("/products/{id}")
    public Result<GoodsVO> updateProduct(@PathVariable Long id,
                                         @Valid @RequestBody GoodsUpdateDTO dto) {
        return Result.success(goodsService.adminUpdate(id, dto));
    }

    /** 上架 / 下架切换：PATCH /api/admin/products/{id}/on-shelf（body: {"onShelf": false}） */
    @PatchMapping("/products/{id}/on-shelf")
    public Result<GoodsVO> toggleOnShelf(@PathVariable Long id,
                                         @Valid @RequestBody OnShelfRequest request) {
        return Result.success(goodsService.adminToggleOnShelf(id, request.onShelf()));
    }

    /**删除商品*/
    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        goodsService.adminDelete(id);
        return Result.success();
    }

    /** 全部订单列表：GET /api/admin/orders?status=&cursor=&size=（游标分页，含 username） */
    @GetMapping("/orders")
    public Result<PageVO<OrderVO>> orders(@RequestParam(required = false) OrderStatusEnum status,
                                          @RequestParam(required = false) Long cursor,
                                          @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.adminListOrders(status, cursor, size));
    }

    /** 订单详情：GET /api/admin/orders/{id}（管理员可查看任意用户订单） */
    @GetMapping("/orders/{id}")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        return Result.success(orderService.getMyOrderDetail(id));
    }

    /** 标记发货：POST /api/admin/orders/{id}/ship（仅 PAID → SHIPPED） */
    @PostMapping("/orders/{id}/ship")
    public Result<OrderVO> ship(@PathVariable Long id) {
        return Result.success(orderService.adminShip(id));
    }

    /** 上下架请求体（api-spec：{"onShelf": false}），仅用于参数接收 */
    public record OnShelfRequest(@NotNull(message = "onShelf 不能为空") Boolean onShelf) {
    }
}
