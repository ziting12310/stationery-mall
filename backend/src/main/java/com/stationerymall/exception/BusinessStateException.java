package com.stationerymall.exception;

import com.stationerymall.enums.BizCodeEnum;

/**
 * 业务状态不合法异常（映射 {@link BizCodeEnum#BUSINESS_STATE}，code=400）。
 * <p>
 * 触发：订单状态流转越权（取消已支付订单、对终态订单发货）、重复评价、
 * 未完成订单评价、下架商品加购等「当前状态不允许该操作」。
 */
public class BusinessStateException extends RuntimeException {

    /** 对应业务码（默认 400；可指定 CONFLICT 等其它业务码复用本类） */
    private final BizCodeEnum bizCode;

    public BusinessStateException() {
        super(BizCodeEnum.BUSINESS_STATE.getMsg());
        this.bizCode = BizCodeEnum.BUSINESS_STATE;
    }

    public BusinessStateException(String msg) {
        super(msg);
        this.bizCode = BizCodeEnum.BUSINESS_STATE;
    }

    /**
     * 指定业务码构造：例如「重复用户名 / 重复评价」复用本类并返回 409。
     *
     * @param bizCode 业务码（决定 HTTP 状态与 body.code）
     * @param msg     业务提示
     */
    public BusinessStateException(BizCodeEnum bizCode, String msg) {
        super(msg);
        this.bizCode = bizCode;
    }

    public BizCodeEnum getBizCode() {
        return bizCode;
    }
}
