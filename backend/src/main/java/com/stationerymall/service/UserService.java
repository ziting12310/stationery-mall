package com.stationerymall.service;

import com.stationerymall.dto.LoginDTO;
import com.stationerymall.dto.RegisterDTO;
import com.stationerymall.vo.LoginVO;
import com.stationerymall.vo.UserVO;

/**
 * 用户服务：注册、登录、登出、当前用户信息。
 */
public interface UserService {

    /**
     * 注册普通用户（角色强制 NORMAL_USER，密码 BCrypt 哈希后入库）。
     *
     * @throws com.stationerymall.exception.BusinessStateException 用户名已存在（code=409）
     */
    UserVO register(RegisterDTO dto);

    /**
     * 登录：校验密码后签发 JWT（payload 含 userId / username / role / jti / exp）。
     *
     * @throws com.stationerymall.exception.UnauthException 用户名或密码错误
     */
    LoginVO login(LoginDTO dto);

    /**
     * 登出：使当前用户全部 token 失效。
     *
     * @throws com.stationerymall.exception.UnauthException 未登录
     */
    void logout();

    /** 当前登录用户信息（不含密码） */
    UserVO currentUser();
}
