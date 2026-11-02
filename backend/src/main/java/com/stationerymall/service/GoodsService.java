package com.stationerymall.service;

import com.stationerymall.dto.GoodsSaveDTO;
import com.stationerymall.dto.GoodsUpdateDTO;
import com.stationerymall.vo.DetailVO;
import com.stationerymall.vo.GoodsVO;
import com.stationerymall.vo.PageVO;

import java.util.List;

/**
 * 商品服务：前台浏览（仅上架） + 后台管理（含下架）。
 */
public interface GoodsService {

    /** 前台商品列表：只返回上架商品，可按二级分类筛选，游标分页 */
    PageVO<GoodsVO> listOnSale(Long categoryId, Long cursor, Integer size);

    /** 商品名称模糊搜索（仅上架，游标分页；关键词为空返回空页） */
    PageVO<GoodsVO> searchByKeyword(String keyword, Long cursor, Integer size);

    /** 首页推荐商品（仅在架商品中随机取，最多 8 件） */
    List<GoodsVO> listRecommended();

    /**
     * 商品详情（含评价列表、评价数、平均分）。
     * <p>
     * 对齐 api-spec 3.3.3：下架商品不报错，返回 {@code onShelf = false}，
     * 并由 Service 生成提示语（Controller 直接透传，不做判断）。
     *
     * @throws com.stationerymall.exception.BusinessStateException 商品不存在（404）
     */
    DetailVO<GoodsVO> getDetail(Long id);

    /** 后台商品列表：含已下架，可按分类与上下架状态筛选（仅管理员） */
    PageVO<GoodsVO> adminList(Long categoryId, Boolean onShelf, Long cursor, Integer size);

    /** 后台新增商品（仅管理员） */
    GoodsVO adminCreate(GoodsSaveDTO dto);

    /** 后台编辑商品（仅管理员） */
    GoodsVO adminUpdate(Long id, GoodsUpdateDTO dto);

    /** 删除商品 */
    GoodsVO adminDelete(Long id);

    /** 后台上架 / 下架切换（仅管理员） */
    GoodsVO adminToggleOnShelf(Long id, boolean onShelf);

    /**
     * 商品上架（仅管理员）：状态改为上架。
     * <p>
     * 角色取自 JWT 登录态，NORMAL_USER 直接 403「无管理员权限，不能操作商品上下架」。
     */
    GoodsVO shelfOn(Long id);

    /**
     * 商品下架（仅管理员）：状态改为下架。
     * <p>
     * 角色取自 JWT 登录态，NORMAL_USER 直接 403「无管理员权限，不能操作商品上下架」。
     */
    GoodsVO shelfOff(Long id);
}
