package com.stationerymall.enums;

/**
 * 订单状态枚举（5 态固定，与 GLOSSARY 2.2 一致）。
 * <pre>
 * UNPAID ──模拟付款──► PAID ──管理员发货──► SHIPPED ──确认收货──► COMPLETED（终态）
 *    │
 *    └──取消（库存回补）──► CANCELLED（终态）
 * </pre>
 * COMPLETED / CANCELLED 为终态，任何角色不得再变更。
 */
public enum OrderStatusEnum {

    /** 待支付：初始状态，可取消、可模拟付款 */
    UNPAID(0, "待支付"),

    /** 已支付：等待管理员发货 */
    PAID(1, "已支付"),

    /** 已发货：等待用户确认收货 */
    SHIPPED(2, "已发货"),

    /** 已完成：终态，可评价 */
    COMPLETED(3, "已完成"),

    /** 已取消：终态，取消时库存回补 */
    CANCELLED(4, "已取消");

    /** DB 存储值 */
    private final int code;

    /** 中文描述 */
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /* ==================== 状态能力判断（Service 层前置校验直接调用） ==================== */

    /** 是否终态（已完成 / 已取消） */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** 是否允许取消（仅待支付） */
    public boolean canCancel() {
        return this == UNPAID;
    }

    /** 是否允许模拟付款（仅待支付） */
    public boolean canPay() {
        return this == UNPAID;
    }

    /** 是否允许管理员标记发货（仅已支付） */
    public boolean canShip() {
        return this == PAID;
    }

    /** 是否允许确认收货（仅已发货） */
    public boolean canConfirmReceipt() {
        return this == SHIPPED;
    }

    /** 是否允许评价（仅已完成） */
    public boolean canEvaluate() {
        return this == COMPLETED;
    }

    /**
     * 判断能否流转到目标状态（订单状态机的唯一事实来源）。
     *
     * @param target 目标状态
     * @return false 表示非法流转，调用方应抛 BusinessStateException
     */
    public boolean canTransitTo(OrderStatusEnum target) {
        if (target == null) {
            return false;
        }
        switch (this) {
            case UNPAID:
                return target == PAID || target == CANCELLED;
            case PAID:
                return target == SHIPPED;
            case SHIPPED:
                return target == COMPLETED;
            default:
                return false; // 终态不允许任何流转
        }
    }

    /** DB 值转枚举；未知值返回 null，由调用方抛业务异常 */
    public static OrderStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
