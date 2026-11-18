package com.chinahitech.shop.utils;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("个人信息分批迁移测试")
class ContactMigrationIntegrationTest {
 @Test @DisplayName("分批迁移 - 损坏数据回滚当前批次且恢复后续跑")
 void testMigration_FailedBatch_Resume() throws Exception {
   // Given - 501条历史数据使迁移跨越两个批次。
   Map<String,String> keys = Map.of("CONTACT_KEY_primary",Base64.getEncoder().encodeToString(new byte[32]),
       "CONTACT_LOOKUP_KEY",Base64.getEncoder().encodeToString(new byte[32]));
   var crypto = new ContactCipher(keys::get);
   try (Connection db = DriverManager.getConnection("jdbc:h2:mem:migration;MODE=MySQL;NON_KEYWORDS=USER", "sa", ""); Statement sql = db.createStatement()) {
     sql.execute("CREATE TABLE `user` (user_id VARCHAR(64) PRIMARY KEY,email TEXT,phone TEXT,user_name TEXT,email_lookup VARCHAR(64),phone_lookup VARCHAR(64),user_name_lookup VARCHAR(64))");
     sql.execute("CREATE TABLE contact_migration_progress(migration_id VARCHAR(80) PRIMARY KEY,last_user_id VARCHAR(255))");
     try (PreparedStatement insert = db.prepareStatement("INSERT INTO `user`(user_id,email) VALUES (?,?)")) {
       for (int i=1;i<=501;i++) { insert.setString(1,String.format("s%04d",i)); insert.setString(2,i==501 ? "enc:broken" : "student@example.com"); insert.addBatch(); }
       insert.executeBatch();
     }
     // When - 第二批失败，第一批与游标已经提交。
     assertThrows(IllegalStateException.class, () -> ContactEncryptionMigration.migrate(db,crypto,"primary"));
     // Then
     try (ResultSet progress = sql.executeQuery("SELECT last_user_id FROM contact_migration_progress")) {
       assertTrue(progress.next()); assertEquals("s0500",progress.getString(1));
     }
     sql.execute("UPDATE `user` SET email='fixed@example.com' WHERE user_id='s0501'"); db.commit();
     ContactEncryptionMigration.migrate(db,crypto,"primary");
     ContactEncryptionMigration.verify(db,crypto,"primary");
     try (ResultSet row = sql.executeQuery("SELECT email,email_lookup FROM `user` WHERE user_id='s0501'")) {
       assertTrue(row.next()); assertEquals("fixed@example.com",crypto.open(row.getString(1),"email",false));
       assertEquals(crypto.digest("fixed@example.com","email"),row.getString(2));
     }
     sql.execute("UPDATE `user` SET email_lookup='corrupt' WHERE user_id='s0501'");
     assertThrows(IllegalStateException.class, () -> ContactEncryptionMigration.verify(db,crypto,"primary"));
   }
 }
}
