package com.chinahitech.shop.mapper;

import com.chinahitech.shop.bean.UserImportTask;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

/** 任务领取与批次检查点均由数据库事务保护，支持多个后端实例。 */
public interface UserImportMapper {
 @Insert({"<script>INSERT INTO `user`(user_id,user_name,user_name_lookup,password,salt,status,create_time,modify_time) VALUES",
 "<foreach collection='users' item='u' separator=','>(#{u.userId},#{u.userName,typeHandler=com.chinahitech.shop.utils.NameTypeHandler},#{u.userNameLookup},#{u.password},#{u.salt},0,NOW(),NOW())</foreach></script>"})
 int insertUsers(@Param("users") java.util.List<com.chinahitech.shop.bean.User> users);

 @Select("SELECT id FROM user_import_guard WHERE id=1 FOR UPDATE") int lockGuard();
 @Select("SELECT COUNT(*) FROM user_import_task WHERE status IN ('PENDING','PROCESSING')") int pendingCount();
 @Insert("INSERT INTO user_import_task(id,uploader,file_path,status) VALUES(#{id},#{uploader},#{filePath},'PENDING')") void create(UserImportTask task);
 @Update("UPDATE user_import_task SET status='PENDING',owner=NULL WHERE status='PROCESSING' AND lease_until < NOW(6)") int recover();
 @Select("SELECT COUNT(*) FROM user_import_task WHERE status='PROCESSING'") int activeCount();
 @Select("SELECT * FROM user_import_task WHERE status='PENDING' ORDER BY created_at,id LIMIT 1 FOR UPDATE") UserImportTask next();
 @Update("UPDATE user_import_task SET status='PROCESSING',owner=#{owner},lease_until=TIMESTAMPADD(SECOND,120,NOW(6)),updated_at=NOW(6) WHERE id=#{id} AND status='PENDING'") int claim(@Param("id") String id,@Param("owner") String owner);
 @Select("SELECT * FROM user_import_task WHERE id=#{id} AND owner=#{owner} AND status='PROCESSING' AND lease_until >= NOW(6) FOR UPDATE") UserImportTask lockOwned(@Param("id") String id,@Param("owner") String owner);
 @Update("UPDATE user_import_task SET lease_until=TIMESTAMPADD(SECOND,120,NOW(6)) WHERE id=#{id} AND owner=#{owner} AND status='PROCESSING' AND lease_until >= NOW(6)") int heartbeat(@Param("id") String id,@Param("owner") String owner);
 @Update("UPDATE user_import_task SET processed=#{processed},succeeded=#{succeeded},duplicates=#{duplicates},invalid=#{invalid},lease_until=TIMESTAMPADD(SECOND,120,NOW(6)),updated_at=NOW(6) WHERE id=#{id} AND owner=#{owner}") void progress(UserImportTask task);
 @Update("UPDATE user_import_task SET status=#{status},total=#{total},error_message=#{errorMessage},updated_at=NOW(6) WHERE id=#{id} AND owner=#{owner} AND status='PROCESSING'") void finish(UserImportTask task);
 @Select("SELECT * FROM user_import_task WHERE id=#{id} AND uploader=#{uploader}") UserImportTask find(@Param("id") String id,@Param("uploader") String uploader);
 @Select("SELECT * FROM user_import_task WHERE uploader=#{uploader} ORDER BY created_at DESC LIMIT 20") List<UserImportTask> list(String uploader);
 @Insert("INSERT INTO user_import_error(task_id,row_no,reason) VALUES(#{id},#{row},#{reason})") void error(@Param("id") String id,@Param("row") long row,@Param("reason") String reason);
 @Select("SELECT row_no,reason FROM user_import_error WHERE task_id=#{id} AND row_no > #{after} ORDER BY row_no LIMIT 100") List<Map<String,Object>> errors(@Param("id") String id,@Param("after") long after);
}
