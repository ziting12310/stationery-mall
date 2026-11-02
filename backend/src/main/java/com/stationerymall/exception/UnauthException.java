package com.stationerymall.exception;

import com.stationerymall.enums.BizCodeEnum;

/**
 * 未登录 / 登录态失效异常（映射 {@link BizCodeEnum#UNAUTH}，code=401）。
 * <p>
 * 触发：请求未携带 token、JWT 验签失败 / 已过期、已登出（jti 命中黑名单）。
 */
public class UnauthException extends RuntimeException {

    /** 对应业务码 */
    private final BizCodeEnum bizCode = BizCodeEnum.UNAUTH;

    public UnauthException() {
        super(BizCodeEnum.UNAUTH.getMsg());
    }

    public UnauthException(String msg) {
        super(msg);
    }

    public BizCodeEnum getBizCode() {
        return bizCode;
    }
}
