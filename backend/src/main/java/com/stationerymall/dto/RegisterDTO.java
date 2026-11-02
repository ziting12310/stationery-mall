package com.stationerymall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求入参。
 * <p>
 * 只含用户名与密码：<b>不提供 role 字段</b>，后端强制写入 NORMAL_USER，
 * 前端无法注册管理员（PRD R-03 / R-04）。
 */
@Data
public class RegisterDTO {

    /** 用户名：3~32 字符，全局唯一 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需 3~32 字符")
    private String username;

    /** 明文密码：≥ 8 字符（BCrypt 输入上限 72 字节） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度需 8~72 字符")
    private String password;
}
