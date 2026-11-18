package com.chinahitech.shop.utils;
import java.sql.*;
import java.util.*;

/** 停写、备份并执行扩列脚本后运行。每批提交，可从最后成功的编号恢复。 */
public final class ContactEncryptionMigration {
 public static void main(String[] args) throws Exception {
    ContactCipher crypto = ContactCipher.migrationCipher();
    crypto.open(crypto.seal("check", "email"), "email", true);
    crypto.digest("check", "email");
    try (Connection db = DriverManager.getConnection(System.getenv("CONTACT_MIGRATION_JDBC_URL"),
            System.getenv("CONTACT_MIGRATION_DB_USER"), System.getenv("CONTACT_MIGRATION_DB_PASSWORD"))) {
      if (args.length == 1 && args[0].equals("--verify")) verify(db, crypto,
              System.getenv().getOrDefault("CONTACT_ACTIVE_KEY_ID", "primary"));
      else if (args.length == 0) migrate(db, crypto, System.getenv().getOrDefault("CONTACT_ACTIVE_KEY_ID", "primary"));
      else throw new IllegalArgumentException("Usage: ContactEncryptionMigration [--verify]");
    }
 }
 /** 全量核验格式、认证标签、目标密钥版本及摘要；不输出任何个人信息。 */
 static void verify(Connection db, ContactCipher crypto, String targetVersion) throws Exception {
    long count = 0;
    try (Statement query = db.createStatement();
         ResultSet rows = query.executeQuery("SELECT email,phone,user_name,email_lookup,phone_lookup,user_name_lookup FROM `user`")) {
      while (rows.next()) {
        for (String[] pair : List.of(new String[]{"email","email"}, new String[]{"phone","phone"}, new String[]{"user_name","userName"})) {
          String value = rows.getString(pair[0]);
          if (value != null && !value.isEmpty() && !value.startsWith("enc:v2:" + targetVersion + ":"))
            throw new IllegalStateException("Contact migration format verification failed");
          String plain = crypto.open(value, pair[1], false);
          if (!Objects.equals(crypto.digest(plain, pair[1]), rows.getString(pair[0] + "_lookup")))
            throw new IllegalStateException("Contact migration lookup verification failed");
        }
        count++;
      }
    }
    System.out.println("Verified user rows: " + count);
 }
 static void migrate(Connection db, ContactCipher crypto, String targetVersion) throws Exception {
    String cursor = "";
      db.setAutoCommit(false);
      try (PreparedStatement progress = db.prepareStatement("SELECT last_user_id FROM contact_migration_progress WHERE migration_id=?")) {
        progress.setString(1, "contact-v2:" + targetVersion);
        try (ResultSet saved = progress.executeQuery()) { if (saved.next()) cursor = saved.getString(1); }
      }
      while (true) {
        List<String[]> batch = new ArrayList<>();
        try (PreparedStatement select = db.prepareStatement("SELECT user_id,email,phone,user_name FROM `user` WHERE user_id > ? ORDER BY user_id LIMIT 500 FOR UPDATE")) {
          select.setString(1, cursor);
          try (ResultSet rows = select.executeQuery()) {
            while (rows.next()) batch.add(new String[]{rows.getString(1), rows.getString(2), rows.getString(3),rows.getString(4)});
          }
        }
        if (batch.isEmpty()) { db.commit(); break; }
        try (PreparedStatement update = db.prepareStatement("UPDATE `user` SET email=?,phone=?,email_lookup=?,phone_lookup=?,user_name=?,user_name_lookup=? WHERE user_id=?")) {
          for (String[] row : batch) {
            String email = crypto.open(row[1], "email", true), phone = crypto.open(row[2], "phone", true);
            update.setString(1, crypto.seal(email, "email")); update.setString(2, crypto.seal(phone, "phone"));
            update.setString(3, crypto.digest(email, "email")); update.setString(4, crypto.digest(phone, "phone"));
            String name = crypto.open(row[3], "userName", true);
            update.setString(5, crypto.seal(name, "userName")); update.setString(6, crypto.digest(name, "userName"));
            update.setString(7, row[0]); update.addBatch();
          }
          update.executeBatch();
          cursor = batch.getLast()[0];
          try (PreparedStatement progress = db.prepareStatement("INSERT INTO contact_migration_progress(migration_id,last_user_id) VALUES (?,?) ON DUPLICATE KEY UPDATE last_user_id=VALUES(last_user_id)")) {
            progress.setString(1, "contact-v2:" + targetVersion);
            progress.setString(2, cursor); progress.executeUpdate();
          }
          db.commit();
          // 编号可能也是敏感信息，进度只输出已提交批次，不记录联系方式。
          System.out.println("Committed batch: " + batch.size() + "; save resume cursor securely from database progress");
        } catch (Exception e) { db.rollback(); throw e; }
      }

 }
}
