package com.stationerymall.exception;

import com.stationerymall.enums.BizCodeEnum;

/**
 * 无权限异常（映射 {@link BizCodeEnum#FORBIDDEN}，code=403）。
 * <p>
 * 触发：普通用户访问后台接口、访问他人订单、管理员尝试下单。
 * 前端隐藏按钮只是体验优化，最终拒绝必须由后端抛出本异常。
 */
public class ForbiddenException extends RuntimeException {

    /** 对应业务码 */
    private final BizCodeEnum bizCode = BizCodeEnum.FORBIDDEN;

    public ForbiddenException() {
        super(BizCodeEnum.FORBIDDEN.getMsg());
    }

    public ForbiddenException(String msg) {
        super(msg);
    }

    public BizCodeEnum getBizCode() {
        return bizCode;
    }
}
