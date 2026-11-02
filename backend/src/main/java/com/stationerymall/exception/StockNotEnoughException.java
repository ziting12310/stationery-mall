package com.stationerymall.exception;

import com.stationerymall.enums.BizCodeEnum;

/**
 * 库存不足异常（映射 {@link BizCodeEnum#STOCK_NOT_ENOUGH}，code=400）。
 * <p>
 * 触发：加购数量超过库存、下单库存不足、并发超卖（UPDATE 影响行数为 0）。
 * 配合 {@code @Transactional(rollbackFor = Exception.class)} 保证整单回滚。
 */
public class StockNotEnoughException extends RuntimeException {

    /** 对应业务码 */
    private final BizCodeEnum bizCode = BizCodeEnum.STOCK_NOT_ENOUGH;

    public StockNotEnoughException() {
        super(BizCodeEnum.STOCK_NOT_ENOUGH.getMsg());
    }

    public StockNotEnoughException(String msg) {
        super(msg);
    }

    /**
     * 便捷构造：统一库存不足话术。
     *
     * @param goodsName 商品名称
     * @param stock     剩余库存
     */
    public StockNotEnoughException(String goodsName, int stock) {
        super(String.format("商品【%s】库存不足，仅剩 %d 件", goodsName, stock));
    }

    public BizCodeEnum getBizCode() {
        return bizCode;
    }
}
