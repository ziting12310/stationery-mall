package com.stationerymall.vo;

import lombok.Data;

/**
 * 带业务提示语的详情包装（如商品详情「该商品已下架，暂不可购买」）。
 * <p>
 * 目的：提示语由 Service 依据业务状态生成，Controller 只做透传，
 * 避免在 Controller 层写「商品是否下架」这类业务判断。
 *
 * @param <T> 业务数据体类型
 */
@Data
public class DetailVO<T> {

    /** 业务提示语（顶层 message，成功时通常为 "ok"） */
    private String message;

    /** 业务数据体 */
    private T data;

    public static <T> DetailVO<T> of(String message, T data) {
        DetailVO<T> detail = new DetailVO<>();
        detail.setMessage(message);
        detail.setData(data);
        return detail;
    }
}
