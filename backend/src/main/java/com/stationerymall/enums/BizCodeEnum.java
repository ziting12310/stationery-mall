package com.stationerymall.enums;

/**
 * 业务状态码枚举（与 docs/02-design/api-spec.md 1.4 节一致）。
 * <p>
 * 约定：{@code code} 与 HTTP 状态码同值，前端既可看 HTTP 状态也可看 body.code。
 * 说明：库存不足与业务状态非法在 api-spec 中统一归一为 400（业务不允许），
 * 这里用不同枚举项区分语义，对外 code 相同，前端靠 {@code msg} 展示具体原因。
 */
public enum BizCodeEnum {

    /** 成功 */
    SUCCESS(0, "ok"),

    /** 参数校验失败 / 请求参数不合法 */
    PARAM_INVALID(400, "参数校验失败"),

    /** 未登录 / 登录态失效（token 缺失、无效或已登出） */
    UNAUTH(401, "未登录或登录态已失效，请重新登录"),

    /** 已登录但无权限（角色不符或访问他人资源） */
    FORBIDDEN(403, "无权限访问该资源"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 业务冲突：重复用户名 / 重复评价 */
    CONFLICT(409, "数据已存在，请勿重复提交"),

    /** 请求体过大：单图超过 5MB */
    PAYLOAD_TOO_LARGE(413, "上传文件过大"),

    /** 库存不足（作业约定：业务不允许统一 400） */
    STOCK_NOT_ENOUGH(400, "库存不足"),

    /** 当前业务状态不允许该操作（订单状态流转越权、下架商品加购等） */
    BUSINESS_STATE(400, "当前业务状态不允许该操作"),

    /** 服务器内部异常（事务失败等，不向前端暴露堆栈） */
    INTERNAL_ERROR(500, "服务器内部异常，请稍后重试");

    /** 业务状态码 */
    private final int code;

    /** 默认提示信息 */
    private final String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
