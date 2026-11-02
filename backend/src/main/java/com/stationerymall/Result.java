package com.stationerymall;

import com.stationerymall.enums.BizCodeEnum;

/**
 * 全局统一返回结构体：{@code { code, message, data }}（对齐 api-spec 1.3）。
 * <p>
 * 所有接口（含失败）均返回该结构；前端 Axios 响应拦截器按 {@code code === 0} 判断成功，
 * 其余情况直接 Toast {@code message}。业务码统一定义在 {@link BizCodeEnum}。
 *
 * @param <T> 业务数据体类型；无数据时为 {@code null}
 */
public class Result<T> {

    /** 业务状态码，0 表示成功 */
    private int code;

    /** 提示信息，前端可直接展示 */
    private String message;

    /** 业务数据体 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /* ==================== 静态工厂 ==================== */

    /** 成功 + 无数据 */
    public static <T> Result<T> success() {
        return new Result<>(BizCodeEnum.SUCCESS.getCode(), BizCodeEnum.SUCCESS.getMsg(), null);
    }

    /** 成功 + 数据体 */
    public static <T> Result<T> success(T data) {
        return new Result<>(BizCodeEnum.SUCCESS.getCode(), BizCodeEnum.SUCCESS.getMsg(), data);
    }

    /** 成功 + 自定义提示 + 数据体 */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(BizCodeEnum.SUCCESS.getCode(), message, data);
    }

    /** 失败（无数据体），msg 取业务码默认提示 */
    public static <T> Result<T> error(BizCodeEnum bizCode) {
        return new Result<>(bizCode.getCode(), bizCode.getMsg(), null);
    }

    /** 失败（自定义提示） */
    public static <T> Result<T> error(BizCodeEnum bizCode, String message) {
        return new Result<>(bizCode.getCode(), message, null);
    }

    /** 失败（携带数据体，例如库存不足时回传不足商品清单） */
    public static <T> Result<T> error(BizCodeEnum bizCode, String message, T data) {
        return new Result<>(bizCode.getCode(), message, data);
    }

    /* ==================== Getter / Setter（Jackson 序列化需要） ==================== */

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
