package com.stationerymall.vo;

import com.stationerymall.enums.OrderStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应 VO（前台「我的订单」与后台订单列表 / 详情共用）。
 * <p>
 * 列表场景可不填 {@code items}，仅用 {@code itemCount} 汇总；详情场景填充 {@code items}。
 */
@Data
public class OrderVO {

    /** 订单 ID */
    private Long id;

    /** 业务订单号 */
    private String orderNo;

    /** 下单用户 ID */
    private Long userId;

    /** 下单用户名（后台订单列表展示用，前台可为 null） */
    private String username;

    /** 订单状态枚举 */
    private OrderStatusEnum status;

    /** 状态中文描述，便于前端直接展示 */
    private String statusDesc;

    /** 订单总金额（服务端重算） */
    private BigDecimal totalAmount;

    /** 收货地址（列表场景可不返回，详情返回） */
    private String address;

    /** 下单备注 */
    private String remark;

    /** 商品件数汇总（明细数量之和），列表展示用 */
    private Integer itemCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 付款时间 */
    private LocalDateTime paidAt;

    /** 发货时间 */
    private LocalDateTime shippedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 取消时间 */
    private LocalDateTime cancelledAt;

    /** 是否可取消（服务端按状态机预计算，前端直接读） */
    private Boolean canCancel;

    /** 是否可确认收货（服务端预计算） */
    private Boolean canConfirmReceipt;

    /** 是否可评价（服务端预计算） */
    private Boolean canEvaluate;

    /** 订单明细（详情场景填充） */
    private List<OrderItemVO> items;
}
