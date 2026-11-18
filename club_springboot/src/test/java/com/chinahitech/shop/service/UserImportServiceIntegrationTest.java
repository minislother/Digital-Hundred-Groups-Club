package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.*;
import com.chinahitech.shop.mapper.UserImportMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.apache.ibatis.session.Configuration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("用户分批导入事务集成测试")
class UserImportServiceIntegrationTest {
 JdbcTemplate db; UserImportMapper mapper; UserImportService service;
 @BeforeEach void setup() throws Exception {
  var source=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1","sa","");
  db=new JdbcTemplate(source);
  db.execute("CREATE TABLE user_import_task(id VARCHAR(36) PRIMARY KEY,uploader VARCHAR(100),file_path VARCHAR(100),status VARCHAR(24),owner VARCHAR(36),lease_until TIMESTAMP,processed BIGINT DEFAULT 0,succeeded BIGINT DEFAULT 0,duplicates BIGINT DEFAULT 0,invalid BIGINT DEFAULT 0,total BIGINT,error_message VARCHAR(500),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
  db.execute("CREATE TABLE user_import_guard(id INT PRIMARY KEY)");db.execute("INSERT INTO user_import_guard VALUES(1)");
  db.execute("CREATE TABLE user_import_error(task_id VARCHAR(36),row_no BIGINT,reason VARCHAR(100),PRIMARY KEY(task_id,row_no))");
  db.execute("CREATE TABLE `user`(user_id VARCHAR(100) PRIMARY KEY,user_name VARCHAR(1024),user_name_lookup VARCHAR(64),password VARCHAR(100),salt VARCHAR(100),status INT,create_time TIMESTAMP,modify_time TIMESTAMP)");
  var config=new Configuration();config.setMapUnderscoreToCamelCase(true);config.addMapper(UserImportMapper.class);
  var factory=new SqlSessionFactoryBean();factory.setDataSource(source);factory.setConfiguration(config);
  mapper=new SqlSessionTemplate(factory.getObject()).getMapper(UserImportMapper.class);
  service=new UserImportService(mapper,new DataSourceTransactionManager(source));
 }
 @AfterEach void close(){service.close();}
 UserImportTask task(String id){
  db.update("INSERT INTO user_import_task(id,uploader,status,owner,lease_until) VALUES(?,?,'PROCESSING',?,TIMESTAMPADD(SECOND,120,CURRENT_TIMESTAMP))",id,"admin",id);
  return mapper.find(id,"admin");
 }
 User user(String id){User u=new User();u.setUserId(id);u.setUserName("测试学生");u.setPassword("password");return u;}
 @Test @DisplayName("分批提交 - 重复账号跳过且重放批次被拒绝")
 void testCommit_DuplicateAndReplay() {
  // Given - 一批包含重复账号及无效行。
  var task=task("a");var stale=mapper.find("a","admin");
  // When - 提交批次。
  service.commit(task,List.of(user("s1"),user("s1"),new User()));
  // Then - 数据与统计一致，旧检查点不能再次提交。
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM `user`",Integer.class));
  var saved=mapper.find("a","admin");assertEquals(3,saved.getProcessed());assertEquals(1,saved.getSucceeded());assertEquals(1,saved.getDuplicates());assertEquals(1,saved.getInvalid());
  assertThrows(IllegalStateException.class,()->service.commit(stale,List.of(user("s2"))));
 }
 @Test @DisplayName("事务回滚 - 当前批失败不撤销前一批")
 void testCommit_CurrentBatchRollback() {
  // Given - 第一批已提交，第二批通过数据库约束制造失败。
  var task=task("a");service.commit(task,List.of(user("s1")));
  db.execute("ALTER TABLE `user` ADD CONSTRAINT reject_bad CHECK(user_id <> 'bad')");
  // When - 第二批发生非重复键错误。
  assertThrows(RuntimeException.class,()->service.commit(task,List.of(user("s2"),user("bad"))));
  // Then - 第一批保留，第二批及其检查点均回滚。
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM `user`",Integer.class));assertEquals(1,mapper.find("a","admin").getProcessed());
 }
 @Test @DisplayName("并发导入 - 两个任务的同一账号只有一条且统计一致")
 void testCommit_ConcurrentSameAccount() throws Exception {
  // Given - 两个任务同时准备相同账号。
  var a=task("a");var b=task("b");var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try {
   // When - 并发提交。
   Future<?> first=pool.submit(()->{await(start);service.commit(a,List.of(user("same")));});
   Future<?> second=pool.submit(()->{await(start);service.commit(b,List.of(user("same")));});
   start.countDown();first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);
   // Then - 唯一约束与重复统计生效。
   assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM `user`",Integer.class));
   assertEquals(1,a.getSucceeded()+b.getSucceeded());assertEquals(1,a.getDuplicates()+b.getDuplicates());
  }finally{pool.shutdownNow();}
 }
 @Test @DisplayName("任务查询 - 非上传人无法读取进度和明细")
 void testGet_OtherUploaderRejected(){
  // Given - 管理员自己的任务。
  task("a");
  // When & Then - 另一账号无法查询。
  assertThrows(com.chinahitech.shop.exception.BusinessException.class,()->service.get("a","other"));
  assertThrows(com.chinahitech.shop.exception.BusinessException.class,()->service.errors("a","other",0));
 }
 @Test @DisplayName("执行租约 - 旧执行者不能提交数据")
 void testCommit_ExpiredLeaseRejected(){
  // Given - 已过期的执行权。
  var task=task("a");db.update("UPDATE user_import_task SET lease_until=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP)");
  // When & Then - 不能继续写入。
  assertThrows(IllegalStateException.class,()->service.commit(task,List.of(user("s1"))));
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM `user`",Integer.class));
 }
 @Test @DisplayName("任务恢复 - 过期任务重新领取后保留检查点")
 void testRecovery_ExpiredTaskKeepsCheckpoint(){
  // Given - 已提交一批但执行租约已过期。
  var task=task("a");service.commit(task,List.of(user("s1")));
  db.update("UPDATE user_import_task SET lease_until=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP)");
  // When - 模拟另一个实例恢复并领取。
  assertEquals(1,mapper.recover());assertEquals(0,mapper.activeCount());
  assertEquals(1,mapper.claim("a","new-owner"));
  // Then - 进度不丢失，旧执行者不能继续写。
  assertEquals(1,mapper.find("a","admin").getProcessed());
  assertThrows(IllegalStateException.class,()->service.commit(task,List.of(user("s2"))));
 }
 static void await(CountDownLatch latch){try{latch.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}}
}
