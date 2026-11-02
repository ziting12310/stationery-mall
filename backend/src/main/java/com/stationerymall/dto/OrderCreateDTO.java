package com.stationerymall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求入参（结算）。
 * <p>
 * <b>刻意不包含 totalAmount</b>：订单金额一律由服务端按当时商品价格重算，
 * 前端传入的金额不采信（PRD R-12）。
 */
@Data
public class OrderCreateDTO {

    /** 勾选的购物车行 ID 列表，至少 1 项；必须全部属于当前用户 */
    @NotEmpty(message = "请至少勾选一件购物车商品")
    private List<Long> cartItemIds;

    /** 收货地址：MVP 纯文本框，≤ 256 字符 */
    @NotBlank(message = "收货地址不能为空")
    @Size(max = 256, message = "收货地址最多 256 字符")
    private String address;

    /** 下单备注，选填，≤ 256 字符 */
    @Size(max = 256, message = "订单备注最多 256 字符")
    private String remark;
}
