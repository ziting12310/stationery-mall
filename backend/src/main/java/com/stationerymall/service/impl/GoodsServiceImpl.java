package com.stationerymall.service.impl;

import com.stationerymall.dto.GoodsSaveDTO;
import com.stationerymall.dto.GoodsUpdateDTO;
import com.stationerymall.entity.Category;
import com.stationerymall.entity.Comment;
import com.stationerymall.entity.Goods;
import com.stationerymall.enums.BizCodeEnum;
import com.stationerymall.enums.GoodsStatusEnum;
import com.stationerymall.enums.UserRoleEnum;
import com.stationerymall.exception.BusinessStateException;
import com.stationerymall.exception.ForbiddenException;
import com.stationerymall.exception.ParamInvalidException;
import com.stationerymall.mapper.CategoryMapper;
import com.stationerymall.mapper.CommentMapper;
import com.stationerymall.mapper.GoodsMapper;
import com.stationerymall.service.GoodsService;
import com.stationerymall.util.UserContext;
import com.stationerymall.vo.CommentVO;
import com.stationerymall.vo.DetailVO;
import com.stationerymall.vo.GoodsVO;
import com.stationerymall.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品服务实现：前台只看上架商品，后台管理需二次角色校验。
 */
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    /** 默认页大小 */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** 页大小上限（对齐 api-spec 1.5） */
    private static final int MAX_PAGE_SIZE = 50;

    /** 首页推荐条数 */
    private static final int RECOMMEND_LIMIT = 8;

    private final GoodsMapper goodsMapper;

    private final CategoryMapper categoryMapper;

    private final CommentMapper commentMapper;

    /** 商品操作日志（SLF4J）：上下架等管理操作记录操作人与目标状态，便于审计排查 */
    private static final Logger log = LoggerFactory.getLogger(GoodsServiceImpl.class);

    @Override
    public PageVO<GoodsVO> listOnSale(Long categoryId, Long cursor, Integer size) {
        int pageSize = normalizeSize(size);
        // 多查 1 条用于判断 hasMore；前台固定追加 on_shelf = 1
        List<Goods> goodsList = goodsMapper.selectList(categoryId, GoodsStatusEnum.ON_SHELF, cursor, pageSize + 1);
        Map<Long, String> categoryNames = loadCategoryNames();
        List<GoodsVO> vos = goodsList.stream()
                .map(goods -> toVO(goods, categoryNames.get(goods.getCategoryId())))
                .toList();
        return PageVO.of(vos, pageSize, GoodsVO::getId);
    }

    @Override
    public PageVO<GoodsVO> searchByKeyword(String keyword, Long cursor, Integer size) {
        // 关键词为空时不查询，直接返回空页（前端也会拦截，这里做服务端兜底）
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            return PageVO.empty();
        }
        int pageSize = normalizeSize(size);
        // 多查 1 条判断 hasMore；前台搜索同样只返回上架商品
        List<Goods> goodsList = goodsMapper.selectByKeyword(trimmed, GoodsStatusEnum.ON_SHELF, cursor, pageSize + 1);
        Map<Long, String> categoryNames = loadCategoryNames();
        List<GoodsVO> vos = goodsList.stream()
                .map(goods -> toVO(goods, categoryNames.get(goods.getCategoryId())))
                .toList();
        return PageVO.of(vos, pageSize, GoodsVO::getId);
    }

    @Override
    public List<GoodsVO> listRecommended() {
        Map<Long, String> categoryNames = loadCategoryNames();
        return goodsMapper.selectRecommended(RECOMMEND_LIMIT).stream()
                .map(goods -> toVO(goods, categoryNames.get(goods.getCategoryId())))
                .toList();
    }

    @Override
    public DetailVO<GoodsVO> getDetail(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "商品不存在");
        }
        // 下架商品也正常返回（onShelf=false），提示语由服务端给出，对齐 api-spec 3.3.3
        GoodsVO vo = toVO(goods, loadCategoryName(goods.getCategoryId()));

        // 内嵌评价：列表 + 总数 + 平均分（服务端计算，前端直接展示）
        List<Comment> comments = commentMapper.selectByProductId(id);
        vo.setEvaluations(comments.stream().map(this::toCommentVO).toList());
        vo.setEvaluationCount(comments.size());
        vo.setAverageRating(averageRating(comments));

        String message = goods.getOnShelf() == GoodsStatusEnum.ON_SHELF
                ? BizCodeEnum.SUCCESS.getMsg() : "该商品已下架，暂不可购买";
        return DetailVO.of(message, vo);
    }

    @Override
    public PageVO<GoodsVO> adminList(Long categoryId, Boolean onShelf, Long cursor, Integer size) {
        requireAdmin();
        int pageSize = normalizeSize(size);
        // onShelf 为 null 表示不限（全部），true 只查上架，false 只查下架
        GoodsStatusEnum status = onShelf == null ? null
                : (onShelf ? GoodsStatusEnum.ON_SHELF : GoodsStatusEnum.OFF_SHELF);
        List<Goods> goodsList = goodsMapper.selectList(categoryId, status, cursor, pageSize + 1);
        Map<Long, String> categoryNames = loadCategoryNames();
        List<GoodsVO> vos = goodsList.stream()
                .map(goods -> toVO(goods, categoryNames.get(goods.getCategoryId())))
                .toList();
        return PageVO.of(vos, pageSize, GoodsVO::getId);
    }

    @Override
    public GoodsVO adminDelete(Long id) {
        requireAdmin();
        // 查询商品
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "商品不存在");
        }
        //物理删除
        goodsMapper.deleteById(id);
        //转VO返回，和其他接口保持一致，复用toVO方法
        Map<Long, String> categoryNames = loadCategoryNames();
        return toVO(goods, categoryNames.get(goods.getCategoryId()));
    }

    @Override
    public GoodsVO adminCreate(GoodsSaveDTO dto) {
        requireAdmin();
        // 商品只能挂二级分类
        validateSecondLevelCategory(dto.getCategoryId());

        Goods goods = new Goods();
        goods.setName(dto.getName());
        goods.setPrice(dto.getPrice());
        goods.setStock(dto.getStock());
        goods.setCategoryId(dto.getCategoryId());
        goods.setDescription(dto.getDescription());
        goods.setImageUrl(dto.getImageUrl());
        // 未传 onShelf 时默认上架
        goods.setOnShelf(Boolean.FALSE.equals(dto.getOnShelf())
                ? GoodsStatusEnum.OFF_SHELF : GoodsStatusEnum.ON_SHELF);
        goodsMapper.insert(goods);
        return getDetail(goods.getId()).getData();
    }

    @Override
    public GoodsVO adminUpdate(Long id, GoodsUpdateDTO dto) {
        requireAdmin();
        Goods exists = goodsMapper.selectById(id);
        if (exists == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "商品不存在");
        }
        if (dto.getCategoryId() != null) {
            validateSecondLevelCategory(dto.getCategoryId());
        }

        // 组装更新对象：Mapper 只更新非 null 字段
        Goods update = new Goods();
        update.setId(id);
        update.setName(dto.getName());
        update.setPrice(dto.getPrice());
        update.setStock(dto.getStock());
        update.setCategoryId(dto.getCategoryId());
        update.setDescription(dto.getDescription());
        update.setImageUrl(dto.getImageUrl());
        goodsMapper.updateById(update);

        // 上下架走独立语句，避免与信息更新混在一起
        if (dto.getOnShelf() != null) {
            goodsMapper.updateOnShelf(id, dto.getOnShelf()
                    ? GoodsStatusEnum.ON_SHELF : GoodsStatusEnum.OFF_SHELF);
        }
        return getDetail(id).getData();
    }

    @Override
    public GoodsVO adminToggleOnShelf(Long id, boolean onShelf) {
        // 原有后台切换接口复用同一套上下架逻辑（权限 + 日志 + 状态更新）
        return changeOnShelf(id, onShelf);
    }

    @Override
    public GoodsVO shelfOn(Long id) {
        return changeOnShelf(id, true);
    }

    @Override
    public GoodsVO shelfOff(Long id) {
        return changeOnShelf(id, false);
    }

    /**
     * 上下架公共逻辑：管理员权限校验 → 操作日志 → 商品存在性校验 → 更新上架状态。
     * <p>
     * 操作人 userId / role 均取自 JWT 登录态（UserContext），前端无法伪造。
     */
    private GoodsVO changeOnShelf(Long id, boolean onShelf) {
        requireShelfAdmin();
        // 操作日志：记录操作人 id、角色、商品 id、操作类型
        log.info("【商品上下架】操作人 userId={}，角色={}，商品 id={}，操作类型={}",
                UserContext.getUserId(), UserContext.getRole(), id, onShelf ? "上架" : "下架");
        Goods exists = goodsMapper.selectById(id);
        if (exists == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "商品不存在");
        }
        // 原有业务更新逻辑保持不变：仅修改商品上架状态字段
        goodsMapper.updateOnShelf(id, onShelf ? GoodsStatusEnum.ON_SHELF : GoodsStatusEnum.OFF_SHELF);
        log.info("【商品上下架】成功：商品 id={}，已改为{}", id, onShelf ? "上架" : "下架");
        return getDetail(id).getData();
    }

    /** 上下架权限校验：只有角色 ADMIN 允许操作；NORMAL_USER → 403「无管理员权限，不能操作商品上下架」 */
    private void requireShelfAdmin() {
        UserRoleEnum role = UserContext.getRole();
        if (!role.isAdmin()) {
            log.warn("【商品上下架】拒绝：操作人 userId={}，角色={}", UserContext.getUserId(), role);
            throw new ForbiddenException("无管理员权限，不能操作商品上下架");
        }
    }

    /* ==================== 私有辅助 ==================== */

    /** 后台操作二次角色校验（兜底防护，不依赖前端隐藏按钮） */
    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new ForbiddenException("仅管理员可执行商品管理操作");
        }
    }

    /** 校验分类存在且为二级分类 */
    private void validateSecondLevelCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessStateException(BizCodeEnum.NOT_FOUND, "商品分类不存在");
        }
        if (category.getParentId() == null) {
            throw new ParamInvalidException("商品只能挂在二级分类下");
        }
    }

    /** 一次性加载全部分类名，避免列表页 N+1 查询 */
    private Map<Long, String> loadCategoryNames() {
        return categoryMapper.selectAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private String loadCategoryName(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    /** 页大小归一：null / 非正数取默认 10，超过上限按 50 处理 */
    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /** 平均评分：保留 1 位小数，无评价返回 0.0 */
    private BigDecimal averageRating(List<Comment> comments) {
        if (comments.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        int sum = comments.stream().mapToInt(Comment::getRating).sum();
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(comments.size()), 1, RoundingMode.HALF_UP);
    }

    /** 评价实体转 VO（username 由 Mapper LEFT JOIN users 回填） */
    private CommentVO toCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setOrderId(comment.getOrderId());
        vo.setProductId(comment.getProductId());
        vo.setUserId(comment.getUserId());
        vo.setUsername(comment.getUsername());
        vo.setRating(comment.getRating());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }

    /** 实体转 VO，派生展示状态 */
    private GoodsVO toVO(Goods goods, String categoryName) {
        GoodsVO vo = new GoodsVO();
        vo.setId(goods.getId());
        vo.setName(goods.getName());
        vo.setPrice(goods.getPrice());
        vo.setStock(goods.getStock());
        vo.setCategoryId(goods.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setDescription(goods.getDescription());
        vo.setImageUrl(goods.getImageUrl());

        Integer onShelfCode = goods.getOnShelf() == null ? null : goods.getOnShelf().getCode();
        int stock = goods.getStock() == null ? 0 : goods.getStock();
        vo.setOnShelf(onShelfCode != null && onShelfCode == 1);
        // 上架 + 库存 0 → ON_SALE_OUT_OF_STOCK；下架 → OFF_SHELF
        vo.setStatus(GoodsStatusEnum.resolve(onShelfCode, stock));
        vo.setCreatedAt(goods.getCreatedAt());
        vo.setUpdatedAt(goods.getUpdatedAt());
        return vo;
    }
}
