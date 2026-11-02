package com.stationerymall.handler;

import com.stationerymall.enums.UserRoleEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * {@link UserRoleEnum} ↔ {@code users.role}（TINYINT）的 MyBatis 类型处理器。
 * <p>
 * 若不注册本处理器，MyBatis 默认会把枚举<b>名字</b>（如 "NORMAL_USER"）写库，与 TINYINT 字段不符。
 * 本类通过 configuration 的 {@code mybatis.type-handlers-package} 全局注册。
 */
@MappedTypes(UserRoleEnum.class)
public class UserRoleTypeHandler extends BaseTypeHandler<UserRoleEnum> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UserRoleEnum parameter, JdbcType jdbcType)
            throws SQLException {
        // VISITOR 不落库（code = null）：写 NULL，避免误写成 0 产生脏角色
        if (parameter.getCode() == null) {
            ps.setNull(i, Types.TINYINT);
        } else {
            ps.setInt(i, parameter.getCode());
        }
    }

    @Override
    public UserRoleEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? UserRoleEnum.VISITOR : UserRoleEnum.fromCode(value);
    }

    @Override
    public UserRoleEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? UserRoleEnum.VISITOR : UserRoleEnum.fromCode(value);
    }

    @Override
    public UserRoleEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? UserRoleEnum.VISITOR : UserRoleEnum.fromCode(value);
    }
}
