package com.stationerymall.config;

import com.stationerymall.exception.ForbiddenException;
import com.stationerymall.exception.UnauthException;
import com.stationerymall.util.JwtBlacklist;
import com.stationerymall.util.JwtUtil;
import com.stationerymall.util.LoginUser;
import com.stationerymall.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token 鉴权拦截器。
 * <p>
 * 只做「身份认证 + 后台角色拦截」，业务归属权校验仍由 Service 层负责：
 * <ol>
 *   <li>公开接口放行：注册、登录、商品与分类公开查询（GET 匿名可访问）、CORS 预检；</li>
 *   <li>其余接口必须携带 {@code Authorization: Bearer <jwt>}；</li>
 *   <li>用 {@link JwtUtil} 验签 + 校验过期，再查 {@link JwtBlacklist}（登出后立即失效）；</li>
 *   <li>{@code /api/admin/**} 强制管理员角色，否则 403；</li>
 *   <li>校验通过后写入 {@link UserContext}，请求结束在 afterCompletion 清理。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    /**
     * 完全无需 token 的路径（注册 / 登录 / 错误页）。
     * WebMvcConfig 的 excludePathPatterns 复用该常量，保证「拦截器判定」与「MVC 排除」同一份定义。
     */
    public static final String[] AUTH_FREE_PATHS = {
            "/api/auth/register",
            "/api/auth/login",
            "/error"
    };

    /**
     * 公开只读路径（商品 / 分类）。
     * 注意：这类路径<b>仍然要经过本拦截器</b>——游客不强制登录，但若携带有效 token 必须解析身份，
     * 否则管理员访问商品详情时 {@code UserContext.isAdmin()} 恒为 false。
     */
    private static final String[] PUBLIC_READ_PATTERNS = {
            "/api/products", "/api/products/**",
            // 商品搜索（公开搜索接口，游客可搜）
            "/api/product", "/api/product/**",
            "/api/categories", "/api/categories/**"
    };

    /**
     * 后台管理接口：强制管理员 token。
     * 在拦截器层直接拦截非管理员请求（403），避免穿透到 Service 才报错。
     */
    private static final String[] ADMIN_PATTERNS = {
            "/api/admin", "/api/admin/**"
    };

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtUtil jwtUtil;

    private final JwtBlacklist jwtBlacklist;

    @Value("${mall.token.header:Authorization}")
    private String tokenHeader;

    @Value("${mall.token.prefix:Bearer }")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. CORS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 解析 JWT：验签 + 过期校验；无 Authorization 头视为游客（loginUser = null）
        String token = resolveToken(request);
        LoginUser loginUser = null;
        if (StringUtils.hasText(token)) {
            loginUser = jwtUtil.parseToken(token);
            // 验签失败 / 已过期 / 已登出（jti 命中黑名单）→ 一律 401
            if (loginUser == null || jwtBlacklist.isRevoked(loginUser.jti())) {
                throw new UnauthException("登录态无效或已过期，请重新登录");
            }
        }

        // 3. 公开只读接口：游客放行；携带有效 token 时同样注入上下文（管理员可见下架商品）
        if (isPublicRead(request)) {
            if (loginUser != null) {
                UserContext.set(loginUser);
            }
            return true;
        }

        // 4. 其余接口必须登录
        if (loginUser == null) {
            throw new UnauthException("未登录或登录态已失效，请重新登录");
        }

        // 5. 后台接口强制管理员角色（普通用户 token 直接 403）
        if (isAdminPath(request) && !loginUser.role().isAdmin()) {
            throw new ForbiddenException("无权限访问后台管理接口");
        }

        // 6. 写入线程上下文，供 Service 读取
        UserContext.set(loginUser);
        log.debug("鉴权通过：userId={}, role={}, uri={}",
                loginUser.userId(), loginUser.role(), request.getRequestURI());
        return true;
    }

    /**
     * 请求结束（无论成功或异常）清理 ThreadLocal，防止线程池复用导致登录态串号。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    /* ==================== 公开路径判定 ==================== */

    /** 是否为公开只读请求（商品 / 分类且方法为 GET） */
    private boolean isPublicRead(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        for (String pattern : PUBLIC_READ_PATTERNS) {
            if (matches(uri, pattern)) {
                return true;
            }
        }
        return false;
    }

    /** 是否为后台管理接口（任意方法都要求管理员） */
    private boolean isAdminPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String pattern : ADMIN_PATTERNS) {
            if (matches(uri, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String uri, String pattern) {
        return pathMatcher.match(pattern, uri);
    }

    /* ==================== token 提取 ==================== */

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(tokenHeader);
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String prefix = tokenPrefix == null ? "" : tokenPrefix;
        if (!prefix.isEmpty() && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        // 兼容未带前缀的裸 token
        return header.trim();
    }
}
