package com.stationerymall.util;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 登出黑名单（简易内存实现）。
 * <p>
 * JWT 本身无状态、无法主动作废，登出时把该 token 的 jti 写入黑名单，
 * 拦截器校验时命中黑名单即视为无效（401）。
 * 说明：进程重启后黑名单清空，过期 token 本身也会被 exp 拦截，MVP 可接受。
 */
@Component
public class JwtBlacklist {

    /** 已注销的 jti 集合 */
    private final Set<String> revokedJti = ConcurrentHashMap.newKeySet();

    /**
     * 登出：把 jti 加入黑名单（幂等）。
     *
     * @param jti JWT 唯一 id
     */
    public void revoke(String jti) {
        if (jti != null && !jti.isBlank()) {
            revokedJti.add(jti);
        }
    }

    /** 该 jti 是否已被注销 */
    public boolean isRevoked(String jti) {
        return jti != null && revokedJti.contains(jti);
    }

    /** 当前黑名单大小（调试用） */
    public int size() {
        return revokedJti.size();
    }
}
