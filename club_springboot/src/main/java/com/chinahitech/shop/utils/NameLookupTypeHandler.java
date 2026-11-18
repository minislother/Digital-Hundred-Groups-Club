package com.chinahitech.shop.utils;
import java.sql.*;
import org.apache.ibatis.type.*;
/** 自定义 SQL 写入时为原值计算独立检索摘要。 */
public final class NameLookupTypeHandler extends BaseTypeHandler<String> {
 public void setNonNullParameter(PreparedStatement ps, int i, String value, JdbcType type) throws SQLException { ps.setString(i, ContactCipher.lookup(value, "userName")); }
 public String getNullableResult(ResultSet rs, String column) throws SQLException { return rs.getString(column); }
 public String getNullableResult(ResultSet rs, int column) throws SQLException { return rs.getString(column); }
 public String getNullableResult(CallableStatement cs, int column) throws SQLException { return cs.getString(column); }
}
