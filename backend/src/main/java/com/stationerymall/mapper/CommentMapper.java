package com.stationerymall.mapper;

import com.stationerymall.entity.Comment;
import com.stationerymall.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评价表 {@code evaluations}（实体 {@link Comment}）的纯数据访问接口。
 * <p>
 * 约束：不判断订单是否已完成、评价人是否为订单所有者（Service 层负责）。
 */
@Mapper
public interface CommentMapper {

    /** 新增评价 */
    int insert(Comment comment);

    /** 按主键查询 */
    Comment selectById(@Param("id") Long id);

    /**
     * 联合唯一校验：统计「订单 + 商品 + 用户」已存在的评价条数。
     * <p>返回 0 表示未评价；返回 &gt;0 表示重复评价，上层应拒绝并返回 409。
     */
    int countByOrderProductUser(@Param("orderId") Long orderId,
                                @Param("productId") Long productId,
                                @Param("userId") Long userId);

    /** 按「订单 + 商品 + 用户」查询评价记录（评价状态接口使用；不存在返回 null） */
    Comment selectByOrderProductUser(@Param("orderId") Long orderId,
                                     @Param("productId") Long productId,
                                     @Param("userId") Long userId);

    /**
     * 按「订单 + 商品」查询评价记录（管理员评价查询使用，不带 user_id 条件）。
     * <p>同一订单商品只会由下单用户评价，取最早一条即可。
     */
    Comment selectByOrderProduct(@Param("orderId") Long orderId,
                                 @Param("productId") Long productId);

    /** 查询某商品的评价列表（商品详情页） */
    List<Comment> selectByProductId(@Param("productId") Long productId);

    /** 查询某订单下的评价（订单详情页标识「已评价」） */
    List<Comment> selectByOrderId(@Param("orderId") Long orderId);

    /** 批量查询多个订单下的评价（订单列表页一次性回填，避免 N+1） */
    List<Comment> selectByOrderIds(@Param("orderIds") List<Long> orderIds);

    /**
     * 评价列表（管理员：userId 传 null 时不拼接 user_id，返回全部用户评价；
     * 普通用户：传本人 userId，仅返回本人评价）。附带订单号 / 商品名称 / 用户名。
     */
    List<CommentVO> selectList(@Param("userId") Long userId);

    /** 按主键删除（MVP 暂不开放，保留基础 CRUD 能力） */
    int deleteById(@Param("id") Long id);
}
