package com.stationerymall.vo;

import com.stationerymall.enums.UserRoleEnum;
import lombok.Data;

/**
 * 登录成功响应 VO。
 * <p>
 * 仅包含 token 与登录用户的最少必要信息；<b>不含密码哈希</b>。
 * 说明：token 为 HS256 签名的 JWT（含过期时间），过期时间在 JWT payload 内，
 * 前端无需单独字段，故不返回 expiresAt。
 */
@Data
public class LoginVO {

    /** 登录 token，前端后续请求放入 {@code Authorization: Bearer <token>} */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色枚举 */
    private UserRoleEnum role;

    /** 角色中文描述 */
    private String roleDesc;
}
