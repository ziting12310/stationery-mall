package com.stationerymall.mapper;

import com.stationerymall.entity.Order;
import com.stationerymall.enums.OrderStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单主表 {@code orders} 的纯数据访问接口。
 * <p>
 * 约束：不判断状态能否流转、不做归属权校验（Service 层负责），只提供读写能力。
 */
@Mapper
public interface OrderMapper {

    /** 新增订单；主键回填到 order.id，供订单明细写入 */
    int insert(Order order);

    /** 按主键查询订单 */
    Order selectById(@Param("id") Long id);

    /**
     * 前台：按用户查询订单列表（游标分页，id 倒序）。
     *
     * @param status 状态筛选，null 表示全部
     * @param cursor 上一页最后一条 id，null 表示第一页
     * @param size   每页条数
     */
    List<Order> selectByUserId(@Param("userId") Long userId,
                               @Param("status") OrderStatusEnum status,
                               @Param("cursor") Long cursor,
                               @Param("size") Integer size);

    /**
     * 后台：查询全部用户订单（可按状态筛选，游标分页，id 倒序）。
     */
    List<Order> selectAll(@Param("status") OrderStatusEnum status,
                          @Param("cursor") Long cursor,
                          @Param("size") Integer size);

    /**
     * 按角色查询订单列表（游标分页，id 倒序）。
     * <p>
     * 权限条件由 XML 动态 SQL 控制：
     * <ul>
     *   <li>{@code userId != null}（普通用户）→ 追加 {@code AND o.user_id = #{userId}}；</li>
     *   <li>{@code userId == null}（管理员）→ 不追加用户条件，返回全部订单。</li>
     * </ul>
     * 说明：userId 由 Service 从 JWT 登录态取得，绝不来自前端请求参数。
     *
     * @param userId 当前登录用户 id；管理员传 null
     * @param status 状态筛选，null 表示全部
     * @param cursor 上一页最后一条 id，null 表示第一页
     * @param size   每页条数
     */
    List<Order> selectList(@Param("userId") Long userId,
                           @Param("status") OrderStatusEnum status,
                           @Param("cursor") Long cursor,
                           @Param("size") Integer size);

    /**
     * 按主键动态更新：状态 / 地址 / 备注 / 各阶段时间戳；null 字段不更新。
     * 状态合法性由 Service 层基于 OrderStatusEnum 判断后再调用。
     */
    int updateById(Order order);
}
