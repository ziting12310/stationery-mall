package com.stationerymall.util;

import com.stationerymall.enums.UserRoleEnum;

/**
 * 登录用户信息（从 JWT 解析得到，随 UserContext 传递给 Service / 拦截器）。
 *
 * @param userId   用户主键
 * @param username 用户名
 * @param role     角色（登录用户最少为 NORMAL_USER，不会为 VISITOR）
 * @param jti      JWT 唯一 id，登出时写入黑名单使该 token 失效
 */
public record LoginUser(Long userId, String username, UserRoleEnum role, String jti) {
}
