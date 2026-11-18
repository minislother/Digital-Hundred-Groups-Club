package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.*;
import com.chinahitech.shop.mapper.UserImportMapper;
import com.chinahitech.shop.utils.*;
import com.chinahitech.shop.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PreDestroy;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;

/** 数据库排队、租约领取和事务检查点；不依赖 Kafka，重启后重读并跳过已提交行。 */
@Service
public class UserImportService {
 private final UserImportMapper mapper;
 private final TransactionTemplate tx;
 @Value("${app.user-import.directory:data/user-import}") private String directory;
 private final ExecutorService workers=Executors.newFixedThreadPool(2);
 private final Map<String,String> running=new ConcurrentHashMap<>();
 @Value("${app.user-import.enabled:false}") private boolean enabled;
 @Value("${app.user-import.global-concurrency:2}") private int concurrency;
 @Value("${app.user-import.max-pending:100}") private int maxPending;
 @Value("${app.user-import.batch-size:500}") private int batchSize;
 @Value("${app.user-import.max-rows:200000}") private long maxRows;
 public UserImportService(UserImportMapper mapper,PlatformTransactionManager manager) {
  this.mapper=mapper;this.tx=new TransactionTemplate(manager);
 }
 public UserImportTask submit(org.springframework.web.multipart.MultipartFile file,String uploader){
  if(!enabled) throw new BusinessException("IMPORT_DISABLED","请先完成导入任务数据库迁移并启用导入服务");
  String name=file==null?null:file.getOriginalFilename();
  if(name==null||!name.toLowerCase(Locale.ROOT).matches(".*\\.xlsx?$")||file.isEmpty())
   throw new BusinessException("FILE_TYPE_INVALID","请选择非空 xls 或 xlsx 文件");
  // 持有很短的队列锁预留容量；文件已由 Servlet 接收到临时目录。
  return tx.execute(status->{
   mapper.lockGuard();
   if(mapper.pendingCount()>=maxPending) throw new BusinessException("IMPORT_BUSY","等待任务过多，请稍后上传");
   // 导入文件含初始密码，必须放在 /upload 静态映射之外。
   Path saved;
   try {
    Path base=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(base);
    String extension=name.toLowerCase(Locale.ROOT).endsWith(".xlsx")?".xlsx":".xls";
    saved=base.resolve(UUID.randomUUID()+extension);
    try(var input=file.getInputStream()){Files.copy(input,saved);}
   }catch(java.io.IOException e){throw new BusinessException("UPLOAD_ERROR","文件保存失败");}
   UserImportTask task=new UserImportTask();task.setId(UUID.randomUUID().toString());
   task.setUploader(uploader);task.setFilePath(saved.toString());task.setStatus("PENDING");
   try {mapper.create(task);} catch(RuntimeException e){try{Files.deleteIfExists(saved);}catch(Exception ignored){} throw e;}
   return task;
  });
 }
 public UserImportTask get(String id,String uploader){
  UserImportTask task=mapper.find(id,uploader);
  if(task==null)throw new BusinessException("NOT_FOUND","导入任务不存在");return task;
 }
 public List<UserImportTask> list(String uploader){return enabled?mapper.list(uploader):Collections.emptyList();}
 public List<Map<String,Object>> errors(String id,String uploader,long after){get(id,uploader);return mapper.errors(id,Math.max(0,after));}
 @Scheduled(fixedDelay=3000)
 public void schedule(){
  if(!enabled)return;
  try{
   running.forEach((id,owner)->mapper.heartbeat(id,owner));
   while(running.size()<2){
    UserImportTask task=tx.execute(status->{
     mapper.lockGuard();mapper.recover();
     if(mapper.activeCount()>=Math.max(1,concurrency))return null;
     UserImportTask next=mapper.next();if(next==null)return null;
     next.setOwner(UUID.randomUUID().toString());
     if(mapper.claim(next.getId(),next.getOwner())!=1)return null;
     return next;
    });
    if(task==null)break;
    running.put(task.getId(),task.getOwner());
    try{workers.submit(()->run(task));}catch(RejectedExecutionException e){running.remove(task.getId());throw e;}
   }
  }catch(Exception e){LoggerFactory.getLogger(getClass()).error("Import scheduler failed",e);}
 }
 private void run(UserImportTask task){
  try{
   long[] seen={0};List<User> batch=new ArrayList<>();
   new ImportExcelReader().read(Path.of(task.getFilePath()),user->{
    if(++seen[0]>maxRows)throw new IllegalArgumentException("文件数据行超过限制");
    if(seen[0]<=task.getProcessed())return;
    batch.add(user);
    if(batch.size()>=Math.max(1,Math.min(1000,batchSize))){commit(task,batch);batch.clear();}
   });
   if(!batch.isEmpty())commit(task,batch);
   if(seen[0]==0)throw new IllegalArgumentException("未发现有效表头或数据，请使用模板");
   task.setTotal(seen[0]);task.setStatus(task.getDuplicates()+task.getInvalid()>0?"PARTIAL_SUCCESS":"SUCCESS");
   mapper.finish(task);
  }catch(Exception e){
   LoggerFactory.getLogger(getClass()).error("Import task failed: {}",task.getId(),e);
   task.setStatus("FAILED");task.setErrorMessage("导入中断，已提交批次保留；请检查文件或联系管理员");mapper.finish(task);
  }finally{running.remove(task.getId());}
 }
 /** 当前批次的数据、错误明细与检查点一同提交；租约锁阻止旧执行者写入。 */
 void commit(UserImportTask task,List<User> batch){
  UserImportTask committed=tx.execute(status->{
   UserImportTask current=mapper.lockOwned(task.getId(),task.getOwner());
   if(current==null)throw new IllegalStateException("任务执行权已失效");
   if(current.getProcessed()!=task.getProcessed())throw new IllegalStateException("检查点发生变化");
   List<User> valid=new ArrayList<>();List<Long> rows=new ArrayList<>();
   for(int i=0;i<batch.size();i++){
    User u=batch.get(i);long row=current.getProcessed()+i+1;
    if(u.getUserId()==null||u.getUserId().isBlank()||u.getUserId().length()>100||u.getUserName()==null||u.getUserName().isBlank()||u.getPassword()==null||u.getPassword().isBlank()){
     current.setInvalid(current.getInvalid()+1);mapper.error(task.getId(),row,"学号、姓名或密码为空，或账号超过长度限制");continue;
    }
    u.setPassword(PasswordUtil.encode(u.getPassword()));u.setSalt(UUID.randomUUID().toString());
    valid.add(u);rows.add(row);
   }
   if(!valid.isEmpty()){
    Object savepoint=status.createSavepoint();
    try{mapper.insertUsers(valid);current.setSucceeded(current.getSucceeded()+valid.size());}
    catch(DuplicateKeyException conflict){
     status.rollbackToSavepoint(savepoint);
     // 仅冲突批次降级逐条写入；每条使用保存点，其他数据库错误仍回滚整个批次。
     for(int i=0;i<valid.size();i++){
      Object one=status.createSavepoint();
      try{mapper.insertUsers(Collections.singletonList(valid.get(i)));current.setSucceeded(current.getSucceeded()+1);}
      catch(DuplicateKeyException duplicate){status.rollbackToSavepoint(one);current.setDuplicates(current.getDuplicates()+1);mapper.error(task.getId(),rows.get(i),"账号已存在或文件中重复");}
      finally{status.releaseSavepoint(one);}
     }
    }finally{status.releaseSavepoint(savepoint);}
   }
   current.setProcessed(current.getProcessed()+batch.size());mapper.progress(current);return current;
  });
  // 事务真正提交后才推进内存检查点。
  task.setProcessed(committed.getProcessed());task.setSucceeded(committed.getSucceeded());task.setDuplicates(committed.getDuplicates());task.setInvalid(committed.getInvalid());
 }
 @PreDestroy public void close(){workers.shutdownNow();}
}
