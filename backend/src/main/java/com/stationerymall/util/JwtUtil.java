package com.stationerymall.util;

import com.stationerymall.enums.UserRoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类（jjwt 0.12.x，HS256 签名）。
 * <p>
 * 职责：
 * <ul>
 *   <li>登录成功后 {@link #createToken} 签发 JWT：payload 含 userId / username / role，
 *       并写入 jti（唯一 id，供登出黑名单使用）、iat、exp；</li>
 *   <li>请求进入时 {@link #parseToken} 验签 + 校验过期，返回 {@link LoginUser}。</li>
 * </ul>
 * 密钥由配置 {@code mall.jwt.secret} 注入（HS256 要求 >= 32 字节）；
 * 过期时间由 {@code mall.jwt.expire-minutes} 注入。
 */
@Component
public class JwtUtil {

    /** HS256 签名密钥 */
    private final SecretKey key;

    /** 过期时间（毫秒） */
    private final long expireMillis;

    public JwtUtil(@Value("${mall.jwt.secret}") String secret,
                   @Value("${mall.jwt.expire-minutes:720}") long expireMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // 启动期直接失败，避免运行时用弱密钥签发 token
            throw new IllegalStateException("mall.jwt.secret 长度必须 >= 32 字节（HS256 要求）");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireMinutes * 60_000L;
    }

    /**
     * 签发 JWT。
     *
     * @param userId   用户主键
     * @param username 用户名
     * @param role     角色（存枚举名：NORMAL_USER / ADMIN）
     * @return 紧凑形式的 JWT 字符串
     */
    public String createToken(Long userId, String username, UserRoleEnum role) {
        Date now = new Date();
        return Jwts.builder()
                // jti：JWT 唯一 id，登出时加入黑名单
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 JWT（验签 + 过期 + 结构）。
     *
     * @param token 去掉 Bearer 前缀后的 JWT 字符串
     * @return 登录用户信息；验签失败 / 过期 / 格式非法时返回 null
     */
    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            UserRoleEnum role = UserRoleEnum.valueOf(claims.get("role", String.class));
            return new LoginUser(userId, username, role, claims.getId());
        } catch (ExpiredJwtException e) {
            // 已过期
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            // 验签失败 / 结构非法 / role 枚举名不存在
            return null;
        }
    }
}
