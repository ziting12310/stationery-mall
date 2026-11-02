package com.stationerymall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品编辑请求入参（PUT /api/admin/products/{id}）。
 * <p>
 * 对齐 api-spec 3.7.3「所有字段可选，传了才更新」：
 * 本 DTO 全部字段允许为 null，只对<b>传了值</b>的字段做格式校验，
 * 具体哪些字段参与更新由 Service 按非 null 判断。
 */
@Data
public class GoodsUpdateDTO {

    /** 商品名称（传了则不能为空串，≤ 128 字符） */
    @Size(min = 1, max = 128, message = "商品名称长度需 1~128 字符")
    private String name;

    /** 单价：DECIMAL(10,2)，必须 > 0 */
    @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "商品价格最多 8 位整数、2 位小数")
    private BigDecimal price;

    /** 库存：≥ 0 整数 */
    @Min(value = 0, message = "库存不能小于 0")
    private Integer stock;

    /** 分类 ID：必须指向二级分类（由 Service 查库校验） */
    @Positive(message = "分类 ID 必须为正整数")
    private Long categoryId;

    /** 商品描述（选填） */
    @Size(max = 2000, message = "商品描述最多 2000 字符")
    private String description;

    /** 商品图片 URL（选填） */
    @Size(max = 512, message = "图片地址最多 512 字符")
    private String imageUrl;

    /** 是否上架：true=上架 / false=下架；不传则不变 */
    private Boolean onShelf;
}
