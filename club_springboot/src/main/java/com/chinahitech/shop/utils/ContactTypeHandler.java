package com.chinahitech.shop.utils;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;

/** 仅绑定联系方式字段，避免影响普通字符串、密码和关联编号。 */
public class ContactTypeHandler extends BaseTypeHandler<String> {
    private final String field;
    public ContactTypeHandler() { this("contact"); }
    protected ContactTypeHandler(String field) { this.field = field; }
    public void setNonNullParameter(PreparedStatement ps, int i, String value, JdbcType type) throws SQLException {
        ps.setString(i, ContactCipher.encrypt(value, field));
    }
    public String getNullableResult(ResultSet rs, String column) throws SQLException { return ContactCipher.decrypt(rs.getString(column), field); }
    public String getNullableResult(ResultSet rs, int column) throws SQLException { return ContactCipher.decrypt(rs.getString(column), field); }
    public String getNullableResult(CallableStatement cs, int column) throws SQLException { return ContactCipher.decrypt(cs.getString(column), field); }
}
