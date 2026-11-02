package com.stationerymall.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册 Token 鉴权拦截器 + CORS 跨域。
 * <p>
 * 拦截范围：{@code /api/**}；
 * 放行路径复用 {@link TokenInterceptor#AUTH_FREE_PATHS}，保证拦截器内部判定与 MVC 排除规则来自同一份定义。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;

    /** 允许跨域的前端地址，逗号分隔（默认 Vite 开发地址） */
    @Value("${mall.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    /** 预检请求缓存时间（秒） */
    @Value("${mall.cors.max-age:3600}")
    private long corsMaxAge;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")
                // 仅注册 / 登录 / 错误页完全排除；商品与分类仍需经过拦截器（游客放行但解析 token）
                .excludePathPatterns(TokenInterceptor.AUTH_FREE_PATHS)
                .order(1);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(corsMaxAge);
    }
}
