package com.stationerymall.controller;

import com.stationerymall.Result;
import com.stationerymall.service.GoodsService;
import com.stationerymall.vo.DetailVO;
import com.stationerymall.vo.GoodsVO;
import com.stationerymall.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台商品接口 {@code /api/products}（公开访问）。
 * <p>
 * 只返回上架商品；下架商品的过滤与可见性判断全部由 Service 处理。
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    /** 商品列表：GET /api/products?categoryId=&cursor=&size=（游标分页，仅上架） */
    @GetMapping
    public Result<PageVO<GoodsVO>> list(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) Long cursor,
                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(goodsService.listOnSale(categoryId, cursor, size));
    }

    /** 首页推荐商品：GET /api/products/recommended（随机取在架商品，data 为数组） */
    @GetMapping("/recommended")
    public Result<List<GoodsVO>> recommended() {
        return Result.success(goodsService.listRecommended());
    }

    /**
     * 商品详情：GET /api/products/{id}（含内嵌评价）。
     * 提示语由 Service 生成（下架时为「该商品已下架，暂不可购买」），Controller 只透传。
     */
    @GetMapping("/{id}")
    public Result<GoodsVO> detail(@PathVariable Long id) {
        DetailVO<GoodsVO> detail = goodsService.getDetail(id);
        return Result.success(detail.getMessage(), detail.getData());
    }

    /**
     * 商品上架：POST /api/products/{id}/on-shelf（仅管理员）。
     * <p>
     * 登录用户 userId / role 由拦截器从 token 解析并写入 UserContext，
     * Controller 只转发，角色校验在 Service（NORMAL_USER → 403）。
     */
    @PostMapping("/{id}/on-shelf")
    public Result<GoodsVO> shelfOn(@PathVariable Long id) {
        return Result.success(goodsService.shelfOn(id));
    }

    /**
     * 商品下架：POST /api/products/{id}/off-shelf（仅管理员）。
     * 权限校验同 {@link #shelfOn(Long)}。
     */
    @PostMapping("/{id}/off-shelf")
    public Result<GoodsVO> shelfOff(@PathVariable Long id) {
        return Result.success(goodsService.shelfOff(id));
    }
}
