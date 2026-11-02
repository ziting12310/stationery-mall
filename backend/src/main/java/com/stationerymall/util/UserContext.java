package com.stationerymall.util;

import com.stationerymall.enums.UserRoleEnum;

/**
 * 当前登录用户上下文（ThreadLocal）。
 * <p>
 * TokenInterceptor 在 preHandle 校验通过后 {@link #set}，
 * 请求结束时在 afterCompletion 中 {@link #clear}，避免线程池复用导致登录态串号。
 * Service / Controller 可直接读取，无需重复解析请求头。
 */
public final class UserContext {

    /** 线程级登录用户 */
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    /** 工具类禁止实例化 */
    private UserContext() {
        throw new UnsupportedOperationException("UserContext 为静态上下文类，禁止实例化");
    }

    /** 写入当前登录用户 */
    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    /** 取当前登录用户（未登录返回 null） */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 取当前登录用户 id（未登录返回 null） */
    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    /** 取当前登录用户名（未登录返回 null） */
    public static String getUsername() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.username();
    }

    /** 取当前登录用户角色（未登录返回 VISITOR，绝不返回 null 造成误判） */
    public static UserRoleEnum getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? UserRoleEnum.VISITOR : user.role();
    }

    /** 是否已登录 */
    public static boolean isLogin() {
        return HOLDER.get() != null;
    }

    /** 是否管理员 */
    public static boolean isAdmin() {
        return getRole().isAdmin();
    }

    /** 取当前请求 JWT 的 jti（登出黑名单使用，未登录返回 null） */
    public static String getJti() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.jti();
    }

    /** 清理当前线程上下文（必须在请求结束时调用，避免线程复用串号） */
    public static void clear() {
        HOLDER.remove();
    }
}
