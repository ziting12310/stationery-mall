package com.stationerymall.handler;

import com.stationerymall.enums.GoodsStatusEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link GoodsStatusEnum} ↔ {@code products.on_shelf}（TINYINT(1)）的 MyBatis 类型处理器。
 * <p>
 * 仅读写持久化状态：1 → ON_SHELF，0 → OFF_SHELF；
 * 「上架-缺货」等派生态不落库，由 {@link GoodsStatusEnum#resolve(Integer, int)} 在 VO 层推导。
 */
@MappedTypes(GoodsStatusEnum.class)
public class GoodsStatusTypeHandler extends BaseTypeHandler<GoodsStatusEnum> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, GoodsStatusEnum parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public GoodsStatusEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? GoodsStatusEnum.OFF_SHELF : GoodsStatusEnum.fromOnShelf(value);
    }

    @Override
    public GoodsStatusEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? GoodsStatusEnum.OFF_SHELF : GoodsStatusEnum.fromOnShelf(value);
    }

    @Override
    public GoodsStatusEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? GoodsStatusEnum.OFF_SHELF : GoodsStatusEnum.fromOnShelf(value);
    }
}
