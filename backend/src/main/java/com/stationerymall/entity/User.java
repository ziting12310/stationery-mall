package com.stationerymall.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stationerymall.enums.UserRoleEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对应表 {@code users}）。
 * <p>
 * 游客（VISITOR）为「未登录」逻辑态，<b>不落库</b>；注册只生成 {@link UserRoleEnum#NORMAL_USER}，
 * 管理员由初始化 SQL 预置。
 * <p>
 * 注意：{@code role} 在 DB 中是 {@code TINYINT}，阶段 3 编写 Mapper 时需注册按 {@code code}
 * 读写的 TypeHandler（MyBatis 默认会按枚举名写入字符串，与表结构不符）。
 */
@Data
public class User {

    /** 主键：users.id */
    private Long id;

    /** 用户名：users.username，全局唯一，登录凭证之一 */
    private String username;

    /**
     * 密码哈希：users.password，BCrypt 结果（60 字符），<b>永不明文存库</b>。
     * <p>
     * 敏感字段：加 {@link JsonIgnore} 防止被误序列化返回前端；
     * 对外展示一律使用 UserVO，禁止直接返回本实体。
     */
    @JsonIgnore
    private String password;

    /** 角色：users.role，DB 仅有 NORMAL_USER(1) / ADMIN(2) */
    private UserRoleEnum role;

    /** 注册时间：users.created_at */
    private LocalDateTime createdAt;

    /** 资料更新时间：users.updated_at */
    private LocalDateTime updatedAt;
}
