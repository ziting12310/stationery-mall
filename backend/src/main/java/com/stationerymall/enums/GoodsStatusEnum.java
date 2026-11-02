package com.stationerymall.enums;

/**
 * 商品状态枚举（与 PRD 第 3 节商品状态机、GLOSSARY 2.3 对齐）。
 * <ul>
 *   <li>落库状态：ON_SHELF(1) / OFF_SHELF(0)，对应 {@code products.on_shelf}；</li>
 *   <li>派生状态：ON_SALE_OUT_OF_STOCK（上架但库存为 0，不落库）；</li>
 *   <li>事务中间态：RESERVED / SOLD（仅下单事务内存在，不落库）；</li>
 *   <li>预留状态：DELETED（MVP 不实现）。</li>
 * </ul>
 * 代码中禁止出现 {@code status == 1} 之类魔法值，统一使用本枚举。
 */
public enum GoodsStatusEnum {

    /** 上架（PRD 中的 ON_SALE）：前台可见，可加购可下单 */
    ON_SHELF(1, "上架", true, true),

    /** 上架但缺货（派生状态）：前台可见，不可加购 */
    ON_SALE_OUT_OF_STOCK(1, "上架-缺货", false, true),

    /** 下单锁定中（事务中间态） */
    RESERVED(null, "下单锁定中", false, false),

    /** 已售出（事务提交后语义） */
    SOLD(null, "已售出", false, true),

    /** 下架：前台不可见，后台可见且可重新上架 */
    OFF_SHELF(0, "下架", true, false),

    /** 已删除（预留，MVP 不实现） */
    DELETED(null, "已删除", false, false);

    /** DB 存储值（products.on_shelf）；null 表示不落库 */
    private final Integer code;

    /** 中文描述 */
    private final String desc;

    /** 是否为可持久化状态（用于 DB 值反查） */
    private final boolean persisted;

    /** 前台（Visitor / NormalUser）是否可见 */
    private final boolean visibleToVisitor;

    GoodsStatusEnum(Integer code, String desc, boolean persisted, boolean visibleToVisitor) {
        this.code = code;
        this.desc = desc;
        this.persisted = persisted;
        this.visibleToVisitor = visibleToVisitor;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isPersisted() {
        return persisted;
    }

    public boolean isVisibleToVisitor() {
        return visibleToVisitor;
    }

    /**
     * 由 DB 字段 on_shelf 反查持久化状态。
     * 仅匹配 persisted 项，避免与派生状态 code 冲突。
     */
    public static GoodsStatusEnum fromOnShelf(Integer onShelf) {
        if (onShelf == null) {
            return OFF_SHELF;
        }
        for (GoodsStatusEnum status : values()) {
            if (status.persisted && status.code != null && status.code.equals(onShelf)) {
                return status;
            }
        }
        return OFF_SHELF;
    }

    /**
     * 结合库存推导展示状态。
     *
     * @param onShelf 1=上架，0=下架
     * @param stock   当前库存
     */
    public static GoodsStatusEnum resolve(Integer onShelf, int stock) {
        if (onShelf == null || onShelf == 0) {
            return OFF_SHELF;
        }
        return stock <= 0 ? ON_SALE_OUT_OF_STOCK : ON_SHELF;
    }
}
