package com.stationerymall.controller;

import com.stationerymall.Result;
import com.stationerymall.dto.LoginDTO;
import com.stationerymall.dto.RegisterDTO;
import com.stationerymall.service.UserService;
import com.stationerymall.vo.LoginVO;
import com.stationerymall.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号接口 {@code /api/auth}（游客可访问注册 / 登录）。
 * <p>
 * 职责边界：只接收参数、触发 {@code @Valid} 校验、调用 Service、包装 Result；
 * 不做任何业务判断（用户名唯一、密码校验、token 失效都由 Service 处理）。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 注册：POST /api/auth/register（注册固定生成普通用户） */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    /** 登录：POST /api/auth/login（成功后由 Service 通过 JwtUtil 签发 JWT） */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /**
     * 登出：POST /api/auth/logout。
     * token 本身由 TokenInterceptor 校验，Service 依据 UserContext 使该用户全部 token 失效。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }
}
