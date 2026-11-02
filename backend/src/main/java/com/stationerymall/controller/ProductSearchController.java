package com.stationerymall.controller;

import com.stationerymall.Result;
import com.stationerymall.service.GoodsService;
import com.stationerymall.vo.GoodsVO;
import com.stationerymall.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品搜索接口 {@code /api/product}（公开访问，与 /api/products 分列但语义独立）。
 * <p>
 * 仅按商品名称模糊匹配上架商品，分页结构与商品列表一致（游标分页）。
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductSearchController {

    private final GoodsService goodsService;

    /**
     * 商品模糊搜索：GET /api/product/search?keyword=&amp;cursor=&amp;size=
     * <p>keyword 为空时返回空页（data 为 {items: [], nextCursor: null, hasMore: false}）。
     */
    @GetMapping("/search")
    public Result<PageVO<GoodsVO>> search(@RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(required = false) Long cursor,
                                          @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(goodsService.searchByKeyword(keyword, cursor, size));
    }
}
