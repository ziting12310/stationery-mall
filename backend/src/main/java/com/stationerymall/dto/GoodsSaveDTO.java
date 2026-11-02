package com.stationerymall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品新增 / 编辑请求入参（后台管理员接口使用）。
 * <p>
 * 新增时全部必填字段生效；编辑（PUT）时按「传了才更新」处理，
 * 因此 Service 层需按实际非 null 字段更新，本 DTO 的基础注解主要服务于新增场景。
 */
@Data
public class GoodsSaveDTO {

    /** 商品名称，≤ 128 字符 */
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 128, message = "商品名称最多 128 字符")
    private String name;

    /** 单价：DECIMAL(10,2)，必须 > 0 */
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "商品价格最多 8 位整数、2 位小数")
    private BigDecimal price;

    /** 库存：≥ 0 整数（DB 为 INT UNSIGNED，禁止负数） */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于 0")
    private Integer stock;

    /** 分类 ID：必须指向二级分类（是否为二级由 Service 层查库校验） */
    @NotNull(message = "商品分类不能为空")
    @Positive(message = "分类 ID 必须为正整数")
    private Long categoryId;

    /** 商品描述，选填 */
    @Size(max = 2000, message = "商品描述最多 2000 字符")
    private String description;

    /** 商品图片 URL，选填，单图 ≤ 512 字符 */
    @Size(max = 512, message = "图片地址最多 512 字符")
    private String imageUrl;

    /** 是否上架：true=上架（默认）/ false=下架 */
    private Boolean onShelf;
}
