package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;
import com.chinahitech.shop.mapper.*;
import com.chinahitech.shop.service.impl.FileStorageServiceImpl;
import com.chinahitech.shop.utils.PageResult;
import com.chinahitech.shop.utils.Result;
import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 真实 MySQL 8 + ES 7.17 集成验证；仅允许显式指定 codex_search_ 前缀的隔离数据库。 */
@DisplayName("活动搜索 - 真实MySQL与ES集成验证")
class ActivitySearchIntegrationTest {
    @TempDir Path upload;
    @Test @DisplayName("搜索全链路 - 中文拼音附件分页及持久化同步恢复")
    void testSearch_RealServices_EndToEnd() throws Exception {
        // Given - 独立测试库和独立随机索引，不依赖生产数据。
        String url = System.getenv("SEARCH_TEST_DB_URL");
        String es = System.getenv("ELASTICSEARCH_SMOKE_URL");
        Assumptions.assumeTrue(url != null && es != null, "需要显式配置隔离测试库和ES");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:13306/codex_search_[a-z_]+\\?.*"));
        DriverManagerDataSource ds = new DriverManagerDataSource(url, "root", "local-benchmark-only");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP TABLE IF EXISTS activity_search_outbox");
        jdbc.execute("DROP TABLE IF EXISTS activity");
        jdbc.execute("CREATE TABLE activity (id INT PRIMARY KEY,name VARCHAR(100),group_name VARCHAR(100)," +
                "organizer VARCHAR(100),description TEXT,attachment VARCHAR(255),hot INT DEFAULT 0," +
                "place VARCHAR(100),is_accepted BOOLEAN) ENGINE=InnoDB");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/activity-search-outbox.sql")).execute(ds);
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(ds);
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(ActivityMapper.class);
        config.addMapper(ActivitySearchOutboxMapper.class);
        factory.setConfiguration(config);
        factory.setMapperLocations(new ClassPathResource("mapper/ActivityMapper.xml"));
        SqlSessionTemplate session = new SqlSessionTemplate(factory.getObject());
        ActivitySearchOutboxMapper outbox = session.getMapper(ActivitySearchOutboxMapper.class);
        FileStorageServiceImpl files = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(files, "uploadDir", upload.toString());
        Files.writeString(upload.resolve("agenda.txt"), "量子纠缠专题独家附件");
        // 真正的二进制 Word/PDF/ZIP 文件，避免只验证 txt 就声称附件抽取可用。
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();
             java.io.OutputStream output = Files.newOutputStream(upload.resolve("sample.docx"))) {
            doc.createParagraph().createRun().setText("机器人竞赛附件");
            doc.write(output);
        }
        assertTrue(files.extractText("/upload/sample.docx").contains("机器人竞赛"));
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage pdfPage = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(pdfPage);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream output = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, pdfPage)) {
                output.beginText(); output.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                output.newLineAtOffset(50, 700); output.showText("attachment-pdf-sentinel"); output.endText();
            }
            doc.save(upload.resolve("sample.pdf").toFile());
        }
        assertTrue(files.extractText("/upload/sample.pdf").contains("attachment-pdf-sentinel"));
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(upload.resolve("sample.zip")))) {
            zip.putNextEntry(new java.util.zip.ZipEntry("note.txt"));
            zip.write("attachment-zip-sentinel".getBytes(java.nio.charset.StandardCharsets.UTF_8)); zip.closeEntry();
        }
        assertTrue(files.extractText("/upload/sample.zip").contains("attachment-zip-sentinel"));
        Files.writeString(upload.resolve("large.txt"), "a".repeat(110000));
        assertTrue(files.extractText("/upload/large.txt").length() <= 100000);
        String index = "codex_search_" + UUID.randomUUID().toString().replace("-", "");
        DataSourceTransactionManager manager = new DataSourceTransactionManager(ds);
        TransactionTemplate tx = new TransactionTemplate(manager);
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create(es))
                .setRequestConfigCallback(c -> c.setConnectTimeout(500).setSocketTimeout(1000)))) {
            ActivitySearchService search = new ActivitySearchService();
            ReflectionTestUtils.setField(search, "activityMapper", session.getMapper(ActivityMapper.class));
            ReflectionTestUtils.setField(search, "outbox", outbox);
            ReflectionTestUtils.setField(search, "fileStorageService", files);
            ReflectionTestUtils.setField(search, "client", client);
            ReflectionTestUtils.setField(search, "index", index);
            ActivitySearchWorker worker = new ActivitySearchWorker(outbox, search, manager);
            worker.initialize();
            for (int i = 1; i <= 125; i++) {
                jdbc.update("INSERT INTO activity VALUES (?,?,?,?,?,?,?,?,?)", i, "人工智能分享会" + i,
                        "计算机协会", "张三", "算法实践", i == 1 ? "/upload/agenda.txt" : null, 0, "教学楼", i != 125);
            }
            // When - 消费真实触发器产生的事件，再进行搜索。
            drain(worker, outbox);
            refresh(client, index);
            // Then - 超过100条的结果可以访问末页，且返回完整业务字段。
            Result result = search.searchPage("人工智能", 7, 20);
            assertEquals("elasticsearch", result.getData().get("searchSource"));
            PageResult<?> page = (PageResult<?>) result.getData().get("page");
            assertEquals(124, page.getTotal());
            assertEquals(7, page.getPages());
            assertEquals(4, page.getRecords().size());
            assertEquals("教学楼", ((Activity) page.getRecords().get(0)).getPlace());
            assertEquals(124, total(search.searchPage("ren gong zhi neng", 1, 20)));
            assertTrue(total(search.searchPage("rengongzhinengfenxianghui1", 1, 20)) > 0);
            assertEquals(1, total(search.searchPage("量子纠缠", 1, 20)));
            assertEquals(124, total(search.searchPage("算法实践", 1, 20)));

            // Given / When / Then - 回滚活动插入时，队列事件也必须回滚。
            tx.execute(s -> { jdbc.update("INSERT INTO activity(id,name,is_accepted) VALUES(999,'回滚活动',1)");
                s.setRollbackOnly(); return null; });
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM activity WHERE id=999", Integer.class));
            assertEquals(0, ((Number) outbox.status().get("pending")).intValue());

            // When - 撤回后尚未同步，搜索主库复核立即过滤；完成同步后总数减少。
            jdbc.update("UPDATE activity SET is_accepted=0 WHERE id=124");
            Result stale = search.searchPage("人工智能", 1, 20);
            assertEquals(1, stale.getData().get("filteredStaleHits"));
            drain(worker, outbox); refresh(client, index);
            assertEquals(123, total(search.searchPage("人工智能", 1, 20)));
            jdbc.update("DELETE FROM activity WHERE id=123");
            drain(worker, outbox); refresh(client, index);
            assertEquals(122, total(search.searchPage("人工智能", 1, 20)));

            // When - 变更附件/名称后应移除旧词并写入新正文。
            Files.writeString(upload.resolve("new.txt"), "深海机器人专属材料");
            jdbc.update("UPDATE activity SET name='航天讲座',attachment='/upload/new.txt' WHERE id=1");
            drain(worker, outbox); refresh(client, index);
            assertEquals(0, total(search.searchPage("量子纠缠", 1, 20)));
            assertEquals(1, total(search.searchPage("深海机器人", 1, 20)));
            assertEquals(1, total(search.searchPage("航天讲座", 1, 20)));

            // When - 模拟不可达的ES，事件留存并退避，查询明确降级；恢复后同一任务完成。
            jdbc.update("UPDATE activity SET name='故障恢复活动' WHERE id=2");
            try (RestHighLevelClient unavailable = new RestHighLevelClient(RestClient.builder(new HttpHost("127.0.0.1", 19201))
                    .setRequestConfigCallback(c -> c.setConnectTimeout(100).setSocketTimeout(100)))) {
                ReflectionTestUtils.setField(search, "client", unavailable);
                assertFalse(worker.consumeOne());
                assertEquals(1, ((Number) outbox.status().get("retrying")).intValue());
                assertEquals(true, search.searchPage("故障恢复", 1, 10).getData().get("degraded"));
            }
            ReflectionTestUtils.setField(search, "client", client);
            jdbc.update("UPDATE activity_search_outbox SET available_at=NOW(3)");
            drain(worker, outbox); refresh(client, index);
            assertEquals(1, total(search.searchPage("故障恢复", 1, 10)));

            // When - 较旧版本事件重放不会覆盖新文档；坏附件不能被假报索引成功。
            long version = client.get(new GetRequest(index, "2"), RequestOptions.DEFAULT).getVersion();
            search.synchronize(2, version - 1);
            assertEquals(version, client.get(new GetRequest(index, "2"), RequestOptions.DEFAULT).getVersion());
            jdbc.update("UPDATE activity SET attachment='/upload/missing.txt' WHERE id=2");
            assertFalse(worker.consumeOne());
            assertEquals(1, ((Number) outbox.status().get("retrying")).intValue());
            Files.writeString(upload.resolve("missing.txt"), "恢复附件");
            jdbc.update("UPDATE activity_search_outbox SET available_at=NOW(3)");
            drain(worker, outbox);
            assertThrows(RuntimeException.class, () -> files.extractText("/upload/../escape.txt"));
            assertEquals("", files.extractText(null));

            // Then - 回填只返回排队数量；持久化事件可由新消费者继续完成。
            assertEquals(124, search.rebuild());
            ActivitySearchWorker restarted = new ActivitySearchWorker(outbox, search, manager);
            drain(restarted, outbox);
            assertEquals(0, ((Number) search.status().get("pending")).intValue());
            // Given / When - 回填与尚未提交的活动修改并发，回填必须等修改提交后再产生新版本。
            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
            java.util.concurrent.CountDownLatch updated = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CountDownLatch commit = new java.util.concurrent.CountDownLatch(1);
            try {
                java.util.concurrent.Future<?> update = pool.submit(() -> tx.execute(s -> {
                    jdbc.update("UPDATE activity SET name='并发回填正确版本' WHERE id=2");
                    updated.countDown();
                    try { if (!commit.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("测试等待超时"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                    return null;
                }));
                assertTrue(updated.await(5, java.util.concurrent.TimeUnit.SECONDS));
                java.util.concurrent.Future<Integer> rebuild = pool.submit(search::rebuild);
                assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> rebuild.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
                commit.countDown(); update.get(10, java.util.concurrent.TimeUnit.SECONDS);
                assertEquals(124, rebuild.get(10, java.util.concurrent.TimeUnit.SECONDS));
                drain(restarted, outbox); refresh(client, index);
                // Then - 更新不会被较高版本号的回填旧快照覆盖。
                assertEquals(1, total(search.searchPage("并发回填正确版本", 1, 10)));
            } finally { commit.countDown(); pool.shutdownNow(); }
            System.out.println("SEARCH_INTEGRATION_OK: 125 fixtures,124 initial hits,last page=4; " +
                    "Chinese,pinyin,attachment,rollback,revoke,delete,update,retry,restart verified. index=" + index);
        }
    }

    private static long total(Result result) { return ((PageResult<?>) result.getData().get("page")).getTotal(); }
    private static void refresh(RestHighLevelClient client, String index) throws Exception {
        client.indices().refresh(new RefreshRequest(index), RequestOptions.DEFAULT);
    }
    private static void drain(ActivitySearchWorker worker, ActivitySearchOutboxMapper outbox) {
        for (int i = 0; i < 1000 && worker.consumeOne(); i++) { }
        assertEquals(0, ((Number) outbox.status().get("pending")).intValue(), "队列应全部成功完成");
    }
}

