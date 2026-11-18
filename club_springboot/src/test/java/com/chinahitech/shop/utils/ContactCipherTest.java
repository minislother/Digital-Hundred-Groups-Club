package com.chinahitech.shop.utils;
import org.junit.jupiter.api.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("联系方式加密与检索测试")
class ContactCipherTest {
 private Map<String,String> config;
 private ContactCipher crypto;
 @BeforeEach void setup() {
   config = new HashMap<>();
   config.put("CONTACT_ACTIVE_KEY_ID", "current");
   config.put("CONTACT_KEY_current", Base64.getEncoder().encodeToString(new byte[32]));
   byte[] other = new byte[32]; Arrays.fill(other, (byte)7);
   config.put("CONTACT_KEY_old", Base64.getEncoder().encodeToString(other));
   config.put("CONTACT_LOOKUP_KEY", Base64.getEncoder().encodeToString(other));
   crypto = new ContactCipher(config::get);
 }
 @Test @DisplayName("加密读写 - 随机密文且正确恢复")
 void testSeal_RandomNonce_RoundTrip() {
   // Given
   String value = "student@example.com";
   // When
   String first = crypto.seal(value, "email"), second = crypto.seal(value, "email");
   // Then
   assertNotEquals(first, second); assertEquals(value, crypto.open(first, "email", false));
 }
 @Test @DisplayName("字段认证 - 跨字段与篡改拒绝解密")
 void testOpen_TamperedOrWrongField_Reject() {
   // Given
   String encrypted = crypto.seal("13800138000", "phone");
   // When & Then
   assertThrows(IllegalStateException.class, () -> crypto.open(encrypted, "email", false));
   assertThrows(IllegalStateException.class, () -> crypto.open(encrypted.substring(0, 20) + "AAAA", "phone", false));
 }
 @Test @DisplayName("密钥轮换 - 新写入使用新版本且旧版本可读")
 void testOpen_Rotation_ReadOld() {
   // Given
   config.put("CONTACT_ACTIVE_KEY_ID", "old");
   String old = crypto.seal("hello", "email");
   // When
   config.put("CONTACT_ACTIVE_KEY_ID", "current");
   String current = crypto.seal("hello", "email");
   // Then
   assertTrue(current.startsWith("enc:v2:current:")); assertEquals("hello", crypto.open(old, "email", false));
   config.remove("CONTACT_KEY_old");
   assertThrows(IllegalStateException.class, () -> crypto.open(old, "email", false));
 }
 @Test @DisplayName("严格读取 - 拒绝明文且迁移可读取历史数据")
 void testOpen_Plaintext_StrictAndMigration() {
   // Given
   String old = "old@example.com";
   // When & Then
   assertThrows(IllegalStateException.class, () -> crypto.open(old, "email", false));
   assertEquals(old, crypto.open(old, "email", true));
   config.put("CONTACT_STRICT_READ", "false");
   assertEquals(old, crypto.open(old, "email", false));
 }
 @Test @DisplayName("历史版本 - 迁移正确读取旧密文")
 void testOpen_LegacyV1_Migration() throws Exception {
   // Given
   byte[] key = new byte[32], nonce = new byte[12];
   config.put("CONTACT_ENCRYPTION_KEY", Base64.getEncoder().encodeToString(key));
   Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key,"AES"), new GCMParameterSpec(128, nonce));
   byte[] encrypted = cipher.doFinal("legacy".getBytes(StandardCharsets.UTF_8));
   byte[] payload = new byte[12 + encrypted.length]; System.arraycopy(encrypted,0,payload,12,encrypted.length);
   String old = "enc:v1:" + Base64.getEncoder().encodeToString(payload);
   // When & Then
   assertEquals("legacy", crypto.open(old,"email",true));
   assertThrows(IllegalStateException.class, () -> crypto.open(old,"email",false));
 }
 @Test @DisplayName("检索摘要 - 规范化域名且区分字段与密钥")
 void testDigest_NormalizationAndSeparation() {
   // Given
   String value = "Student@EXAMPLE.COM";
   // When
   String digest = crypto.digest(value,"email");
   // Then
   assertEquals(64,digest.length()); assertEquals(digest,crypto.digest(" Student@example.com ","email"));
   assertNotEquals(digest,crypto.digest(value,"phone"));
   config.put("CONTACT_LOOKUP_KEY",config.get("CONTACT_KEY_current"));
   assertNotEquals(digest,crypto.digest(value,"email"));
 }
 @Test @DisplayName("配置错误 - 缺密钥与非法密文直接失败")
 void testCrypto_InvalidConfig_Reject() {
   // Given
   config.remove("CONTACT_KEY_current");
   // When & Then
   assertThrows(IllegalStateException.class, () -> crypto.seal("value","email"));
   assertThrows(IllegalStateException.class, () -> crypto.open("enc:v9:bad","phone",true));
   config.put("CONTACT_LOOKUP_KEY","bad");
   assertThrows(IllegalStateException.class, () -> crypto.digest("value","email"));
 }
 @Test @DisplayName("空值边界 - 保留空联系方式")
 void testCrypto_EmptyValues_Preserve() {
   // Given
   String empty = "";
   // When & Then
   assertNull(crypto.seal(null,"phone")); assertEquals(empty,crypto.open(empty,"email",false));
   assertNull(crypto.digest(" ","email"));
 }
}
