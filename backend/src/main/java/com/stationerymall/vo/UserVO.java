package com.stationerymall.vo;

import com.stationerymall.enums.UserRoleEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应 VO。
 * <p>
 * <b>不含密码字段</b>：任何接口返回用户信息时只能使用本 VO，禁止直接序列化 {@code User} 实体。
 */
@Data
public class UserVO {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 角色枚举（NORMAL_USER / ADMIN） */
    private UserRoleEnum role;

    /** 角色中文描述，便于前端直接展示 */
    private String roleDesc;

    /** 注册时间 */
    private LocalDateTime createdAt;
}
