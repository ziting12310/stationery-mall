package com.stationerymall.exception;

import com.stationerymall.enums.BizCodeEnum;

/**
 * 参数校验失败异常（映射 {@link BizCodeEnum#PARAM_INVALID}，code=400）。
 * <p>
 * 触发：手写业务参数校验不通过（分类必须为二级分类、价格必须为正数等）。
 * Jakarta Validation（@Valid）抛出的校验异常也由 GlobalExceptionHandler 归并到同一结构。
 */
public class ParamInvalidException extends RuntimeException {

    /** 对应业务码 */
    private final BizCodeEnum bizCode = BizCodeEnum.PARAM_INVALID;

    public ParamInvalidException() {
        super(BizCodeEnum.PARAM_INVALID.getMsg());
    }

    public ParamInvalidException(String msg) {
        super(msg);
    }

    public BizCodeEnum getBizCode() {
        return bizCode;
    }
}
