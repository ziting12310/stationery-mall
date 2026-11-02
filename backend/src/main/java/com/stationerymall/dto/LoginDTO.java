package com.stationerymall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求入参（普通用户与管理员共用同一登录接口）。
 */
@Data
public class LoginDTO {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名最多 32 字符")
    private String username;

    /** 明文密码，交由 BCryptUtil 与库中哈希比对 */
    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码最多 72 字符")
    private String password;
}
