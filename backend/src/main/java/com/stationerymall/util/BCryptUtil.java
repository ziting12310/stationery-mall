package com.stationerymall.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * 密码哈希工具类（BCrypt）。
 * <p>
 * 红线：users.password 字段<b>绝不存明文</b>，注册 / 登录统一走本工具。
 * 使用 at.favre.lib:bcrypt 独立实现，避免引入 Spring Security 依赖。
 * BCrypt 每次哈希自动生成随机盐，同一明文两次结果不同，只能通过 {@link #matches} 校验。
 */
public final class BCryptUtil {

    /** 计算强度（2^10 轮），兼顾安全与登录耗时 */
    private static final int STRENGTH = 10;

    /** 工具类禁止实例化 */
    private BCryptUtil() {
        throw new UnsupportedOperationException("BCryptUtil 为静态工具类，禁止实例化");
    }

    /**
     * 明文密码 -> BCrypt 哈希（约 60 字符，可写入 VARCHAR(128)）。
     *
     * @param rawPassword 明文密码
     * @return BCrypt 哈希；入参为空时抛 IllegalArgumentException
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("待加密密码不能为空");
        }
        return BCrypt.withDefaults().hashToString(STRENGTH, rawPassword.toCharArray());
    }

    /**
     * 校验明文密码与已存哈希是否匹配。
     *
     * @param rawPassword    明文密码
     * @param hashedPassword DB 中存储的 BCrypt 哈希
     * @return true=匹配；任一为空或哈希格式非法时返回 false（不暴露内部细节）
     */
    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPassword);
            return result.verified;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
