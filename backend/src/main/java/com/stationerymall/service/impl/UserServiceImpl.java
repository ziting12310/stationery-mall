package com.stationerymall.service.impl;

import com.stationerymall.dto.LoginDTO;
import com.stationerymall.dto.RegisterDTO;
import com.stationerymall.entity.User;
import com.stationerymall.enums.BizCodeEnum;
import com.stationerymall.enums.UserRoleEnum;
import com.stationerymall.exception.BusinessStateException;
import com.stationerymall.exception.UnauthException;
import com.stationerymall.mapper.UserMapper;
import com.stationerymall.service.UserService;
import com.stationerymall.util.BCryptUtil;
import com.stationerymall.util.JwtBlacklist;
import com.stationerymall.util.JwtUtil;
import com.stationerymall.util.UserContext;
import com.stationerymall.vo.LoginVO;
import com.stationerymall.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现：注册 / 登录 / 登出 / 当前用户。
 * <p>
 * 登录态为标准 JWT：登录由 {@link JwtUtil} 签发，登出把当前 token 的 jti 写入
 * {@link JwtBlacklist} 黑名单，此后该 token 一律 401。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final JwtUtil jwtUtil;

    private final JwtBlacklist jwtBlacklist;

    @Override
    public UserVO register(RegisterDTO dto) {
        // 1. 用户名唯一性校验（409 冲突）
        User exists = userMapper.selectByUsername(dto.getUsername());
        if (exists != null) {
            throw new BusinessStateException(BizCodeEnum.CONFLICT, "该用户名已被占用");
        }
        // 2. 密码 BCrypt 哈希后入库，绝不存明文
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCryptUtil.encode(dto.getPassword()));
        // 3. 角色强制普通用户：忽略任何外部传入的角色（注册接口没有 role 字段）
        user.setRole(UserRoleEnum.NORMAL_USER);
        userMapper.insert(user);

        // 回查一次以获取 DB 默认生成的 created_at
        return toVO(userMapper.selectById(user.getId()));
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectByUsername(dto.getUsername());
        // 用户不存在与密码错误统一返回同一提示，避免用户名枚举
        if (user == null || !BCryptUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new UnauthException("用户名或密码错误");
        }
        // 签发 JWT（payload：userId / username / role + jti + exp，HS256 签名）
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setRoleDesc(user.getRole() == null ? null : user.getRole().getDesc());
        return vo;
    }

    @Override
    public void logout() {
        // 当前请求的 jti 由 TokenInterceptor 解析 JWT 后写入 UserContext
        String jti = UserContext.getJti();
        if (jti == null) {
            throw new UnauthException();
        }
        // JWT 无法主动作废：jti 加入黑名单，此后该 token 一律 401
        jwtBlacklist.revoke(jti);
    }

    @Override
    public UserVO currentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new UnauthException();
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UnauthException("登录用户不存在");
        }
        return toVO(user);
    }

    /** 实体转 VO（不含 password） */
    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setRoleDesc(user.getRole() == null ? null : user.getRole().getDesc());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
