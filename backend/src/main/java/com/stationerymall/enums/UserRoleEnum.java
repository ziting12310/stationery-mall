package com.stationerymall.enums;

/**
 * 用户角色枚举（与 GLOSSARY.md 2.1 一致）。
 * <p>
 * VISITOR 为「未登录」逻辑态，<b>不落库</b>；NORMAL_USER 为注册默认角色；
 * ADMIN 由初始化脚本预置，禁止自助注册，且禁止下单。
 */
public enum UserRoleEnum {

    /** 游客：仅浏览，不入库 */
    VISITOR(null, "游客"),

    /** 普通用户：注册生成，可购物 */
    NORMAL_USER(1, "普通用户"),

    /** 管理员：可运营后台，禁止下单 */
    ADMIN(2, "管理员");

    /** DB 存储值；null 表示不落库 */
    private final Integer code;

    /** 中文描述 */
    private final String desc;

    UserRoleEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 是否管理员（后台接口鉴权使用） */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /** 是否可下单（管理员禁止下单） */
    public boolean canPlaceOrder() {
        return this == NORMAL_USER;
    }

    /** DB 值转枚举；未知值或 null 保守降级为 VISITOR，避免误判为高权限 */
    public static UserRoleEnum fromCode(Integer code) {
        if (code == null) {
            return VISITOR;
        }
        for (UserRoleEnum role : values()) {
            if (code.equals(role.code)) {
                return role;
            }
        }
        return VISITOR;
    }
}
