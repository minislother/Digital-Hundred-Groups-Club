package com.chinahitech.shop.utils;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.mapper.StuMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("联系方式持久化集成测试")
class ContactStorageIntegrationTest {
 @Test @DisplayName("数据库读写 - 自定义映射与通用更新均保存密文和摘要")
 void testStorage_CustomAndGeneric_MaintainCipherAndDigest() throws Exception {
   // Given - 独立内存库，不连接业务数据库。
   var source = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:contacts;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1", "sa", "");
   try (Connection db = source.getConnection(); Statement sql = db.createStatement()) {
     sql.execute("DROP TABLE IF EXISTS `user`");
     sql.execute("CREATE TABLE `user` (user_id VARCHAR(64) PRIMARY KEY,password VARCHAR(255),user_name TEXT,user_name_lookup VARCHAR(64),email TEXT,phone TEXT,email_lookup VARCHAR(64),phone_lookup VARCHAR(64),salt VARCHAR(64),status INT,campus VARCHAR(255),school VARCHAR(255),major VARCHAR(255),description TEXT,nickname VARCHAR(255),avatar TEXT,create_user VARCHAR(64),modify_user VARCHAR(64),create_time TIMESTAMP,modify_time TIMESTAMP)");
   }
   var config = new MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true);
   config.setEnvironment(new Environment("test", new JdbcTransactionFactory(), source));
   for (String mapperName : java.util.List.of("StuMapper","ManagerMapper","TopManagerMapper","ActivityMapper")) {
     try (var xml = getClass().getResourceAsStream("/mapper/" + mapperName + ".xml")) {
       new XMLMapperBuilder(xml, config, "mapper/" + mapperName + ".xml", config.getSqlFragments()).parse();
     }
   }
   var factory = new MybatisSqlSessionFactoryBuilder().build(config);
   // When - 自定义注册 SQL，通用更新和通用新增。
   try (var session = factory.openSession(true)) {
     var mapper = session.getMapper(StuMapper.class);
     mapper.addStudent("s1", "hash", "Student@example.com", "salt", new Date(), new Date(), 0);
     assertEquals("Student@example.com",mapper.getByNum("s1").getEmail());
     User patch = new User(); patch.setUserId("s1"); patch.setPhone("13800138000"); mapper.updateById(patch);
     User second = new User(); second.setUserId("s2"); second.setEmail("other@example.com"); second.setUserName("张三"); mapper.insert(second);
     assertEquals("13800138000",mapper.selectById("s1").getPhone());
     assertEquals("other@example.com",mapper.getByNum("s2").getEmail());
     assertEquals("s2",mapper.getByName("张三").getUserId());
     mapper.updatePhone("s2","13900139000",new Date());
     var top = session.getMapper(com.chinahitech.shop.mapper.TopManagerMapper.class);
     assertEquals(1,top.countUser("张三"));
     assertEquals("s2",top.getUserPage("张三",0,10).getFirst().getUserId());
     var manager = session.getMapper(com.chinahitech.shop.mapper.ManagerMapper.class);
     manager.addManager("m1","hash","manager@example.com","salt",new Date(),new Date(),1);
     manager.updatePhone("m1","13700137000",new Date());
     assertEquals("13700137000",manager.getByNum("m1").getPhone());
     top.addManager("t1","hash","top@example.com","salt",new Date(),new Date(),10);
     assertEquals("top@example.com",top.getTopManagerByNum("t1").getEmail());
     // 隔离新增身份数据，后续原始SQL断言只检查学生行。
     manager.deleteById("m1"); top.deleteById("t1");
   }
   // Then - 数据库仅见密文，摘要与实际联系方式一致。
   try (Connection db = source.getConnection(); Statement sql = db.createStatement(); ResultSet rows = sql.executeQuery("SELECT * FROM `user` ORDER BY user_id")) {
     assertTrue(rows.next()); assertTrue(rows.getString("email").startsWith("enc:v2:"));
     assertTrue(rows.getString("phone").startsWith("enc:v2:"));
     assertEquals(ContactCipher.lookup("Student@example.com","email"),rows.getString("email_lookup"));
     assertEquals(ContactCipher.lookup("13800138000","phone"),rows.getString("phone_lookup"));
     assertTrue(rows.next()); assertEquals(ContactCipher.lookup("13900139000","phone"),rows.getString("phone_lookup"));
   }
 }
}
