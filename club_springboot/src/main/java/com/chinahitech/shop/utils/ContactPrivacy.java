package com.chinahitech.shop.utils;
import com.chinahitech.shop.bean.User;
import org.springframework.beans.BeanUtils;
import java.util.*;
/** 展示副本脱敏，避免修改 ORM 对象后意外把掩码写回数据库。 */
public final class ContactPrivacy {
 private ContactPrivacy() {}
 public static String mask(String value) {
   if (value == null || value.isEmpty()) return value;
   int at = value.indexOf('@');
   if (at > 0) return value.substring(0,1) + "***" + value.substring(at);
   return value.length() > 7 ? value.substring(0,3) + "****" + value.substring(value.length()-4) : "***";
 }
 public static User summary(User source) {
   User copy = new User(); BeanUtils.copyProperties(source, copy);
   copy.setPhone(mask(source.getPhone())); copy.setEmail(mask(source.getEmail()));
   copy.setPassword(null); copy.setSalt(null); return copy;
 }
 public static List<Map<String,Object>> applicants(List<Map<String,Object>> rows) {
   return rows.stream().map(row -> {
     Map<String,Object> copy = new LinkedHashMap<>(row);
     for (String field : List.of("phone","email")) {
       Object value = copy.get(field); if (value instanceof String text) copy.put(field, mask(text));
     }
     return copy;
   }).toList();
 }
}
