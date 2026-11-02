package com.stationerymall.mapper;

import com.stationerymall.entity.Goods;
import com.stationerymall.enums.GoodsStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品表 {@code products}（实体 {@link Goods}）的纯数据访问接口。
 * <p>
 * 约束：
 * <ul>
 *   <li>不判断商品是否可购买、库存是否足够（那是 Service 层职责）；</li>
 *   <li>库存扣减 / 回写采用行级条件更新，<b>影响行数 = 0 即代表失败</b>，由上层决定抛异常；</li>
 *   <li>商品改名 / 改价不影响历史订单（订单明细使用快照字段）。</li>
 * </ul>
 */
@Mapper
public interface GoodsMapper {

    /** 新增商品 */
    int insert(Goods goods);

    /** 按主键查询（含已下架商品） */
    Goods selectById(@Param("id") Long id);

    /**
     * 分页查询商品列表（游标分页，id 倒序）。
     *
     * @param categoryId 按分类筛选，null 表示不限
     * @param onShelf    上架状态筛选（ON_SHELF / OFF_SHELF），null 表示不限（后台场景）
     * @param cursor     游标：上一页最后一条 id，null 表示第一页
     * @param size       每页条数
     */
    List<Goods> selectList(@Param("categoryId") Long categoryId,
                           @Param("onShelf") GoodsStatusEnum onShelf,
                           @Param("cursor") Long cursor,
                           @Param("size") Integer size);

    /**
     * 按商品名称模糊搜索（仅上架，游标分页，id 倒序）。
     *
     * @param keyword 名称关键词（非空，Service 已 trim）
     * @param onShelf 上架状态筛选（前台传 ON_SHELF）
     * @param cursor  游标：上一页最后一条 id，null 表示第一页
     * @param size    每页条数
     */
    List<Goods> selectByKeyword(@Param("keyword") String keyword,
                                @Param("onShelf") GoodsStatusEnum onShelf,
                                @Param("cursor") Long cursor,
                                @Param("size") Integer size);

    /**
     * 首页推荐商品（随机取上架商品，对齐 api-spec 3.3.2）。
     *
     * @param limit 返回条数上限
     */
    List<Goods> selectRecommended(@Param("limit") Integer limit);

    /** 统计某分类下的商品数（删除分类前由上层判断是否可删） */
    int countByCategoryId(@Param("categoryId") Long categoryId);

    /** 按主键动态更新商品信息（名称 / 价格 / 库存 / 分类 / 描述 / 图片） */
    int updateById(Goods goods);

    /** 仅更新上下架标志（上架 / 下架切换） */
    int updateOnShelf(@Param("id") Long id, @Param("onShelf") GoodsStatusEnum onShelf);

    /** 按主键删除（MVP 物理删除；历史订单明细使用快照，不受影响） */
    int deleteById(@Param("id") Long id);

    /**
     * 行级扣减库存（防超卖核心语句）。
     * <pre>UPDATE products SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}</pre>
     *
     * @return 影响行数；0 表示库存不足或商品不存在，上层需整单回滚
     */
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 行级回补库存（订单取消时使用）。
     * <pre>UPDATE products SET stock = stock + #{quantity} WHERE id = #{id}</pre>
     *
     * @return 影响行数
     */
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
