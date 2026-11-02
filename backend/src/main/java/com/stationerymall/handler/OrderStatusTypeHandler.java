package com.stationerymall.handler;

import com.stationerymall.enums.OrderStatusEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link OrderStatusEnum} ↔ {@code orders.status}（TINYINT）的 MyBatis 类型处理器。
 * <p>
 * 状态流转合法性由 {@link OrderStatusEnum#canTransitTo(OrderStatusEnum)} 在 Service 层判断，
 * 本类只负责枚举与 DB 整数值的互转。
 */
@MappedTypes(OrderStatusEnum.class)
public class OrderStatusTypeHandler extends BaseTypeHandler<OrderStatusEnum> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, OrderStatusEnum parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public OrderStatusEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : OrderStatusEnum.fromCode(value);
    }

    @Override
    public OrderStatusEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : OrderStatusEnum.fromCode(value);
    }

    @Override
    public OrderStatusEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? null : OrderStatusEnum.fromCode(value);
    }
}
