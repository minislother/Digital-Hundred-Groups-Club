package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.ActivityAdmissionOutbox;
import com.chinahitech.shop.constant.MqTopics;
import com.chinahitech.shop.mapper.ActivityAdmissionOutboxMapper;
import com.chinahitech.shop.mapper.ActivityAdmissionReceiptMapper;
import com.chinahitech.shop.mq.KafkaProducer;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/** 用隔离的本机 MySQL 库与独立连接复现两个投递器读到同一事件。 */
@EnabledIfEnvironmentVariable(named = "BAITUAN_RUN_MYSQL_INTEGRATION", matches = "true")
@DisplayName("报名发件箱 MySQL 并发验证")
class ActivityAdmissionOutboxIntegrationTest {
    @Test
    @DisplayName("两个实例抢占同一事件时只有一个发送，旧租约不能更新")
    void testDispatchPending_ConcurrentMySqlClaim_SendsOnce() throws Exception {
        // Given - 随机测试库仅承载本次测试，不使用项目业务库。
        String schema = "baituan_admission_test_" + UUID.randomUUID().toString().replace("-", "");
        String user = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        var server = new JdbcTemplate(dataSource("", user, password));
        server.execute("create database " + schema);
        try {
            var database = dataSource(schema, user, password);
            new ResourceDatabasePopulator(
                    new ClassPathResource("db/flyway/V2__cache_and_admission_outbox.sql"),
                    new ClassPathResource("db/flyway/V6__admission_outbox_last_error.sql"),
                    new ClassPathResource("cache-consistency-schema.sql")).execute(database);
            var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), database));
            config.setMapUnderscoreToCamelCase(true);
            config.addMapper(ActivityAdmissionReceiptMapper.class);
            String mapperPath = "mapper/ActivityAdmissionOutboxMapper.xml";
            try (var xml = new ClassPathResource(mapperPath).getInputStream()) {
                new XMLMapperBuilder(xml, config, mapperPath, config.getSqlFragments()).parse();
            }
            var sessions = new SqlSessionFactoryBuilder().build(config);
            var jdbc = new JdbcTemplate(database);
            var event = new ActivityAdmissionOutbox();
            event.setId("claim-test");
            event.setPayload("{}");
            try (var session = sessions.openSession(true)) {
                session.getMapper(ActivityAdmissionOutboxMapper.class).insert(event);
            }
            var producer = mock(KafkaProducer.class);
            when(producer.sendWithResult(MqTopics.ACTIVITY_ADMISSION, "{}")).thenReturn(KafkaProducer.SendResult.CONFIRMED);
            var bothRead = new CyclicBarrier(2);

            // When - 两个服务对象通过不同数据库会话，读到同一份到期记录后同时认领。
            try (var pool = Executors.newFixedThreadPool(2)) {
                var first = pool.submit(() -> dispatch(sessions, producer, bothRead));
                var second = pool.submit(() -> dispatch(sessions, producer, bothRead));
                first.get(15, TimeUnit.SECONDS);
                second.get(15, TimeUnit.SECONDS);
            }

            // Then
            verify(producer, times(1)).sendWithResult(MqTopics.ACTIVITY_ADMISSION, "{}");
            assertEquals(1, jdbc.queryForObject("select count(*) from activity_admission_outbox where sent_at is not null", Integer.class));

            // Given - 前任实例的租约已过期，重启的实例完成重新认领。
            jdbc.update("update activity_admission_outbox set sent_at = null, next_attempt_at = now() - interval 1 second");
            try (var session = sessions.openSession(true)) {
                var mapper = session.getMapper(ActivityAdmissionOutboxMapper.class);
                var due = mapper.findDue().getFirst();
                assertEquals(1, mapper.claim(due.getId(), due.getNextAttemptAt(), due.getClaimUntil()));
                // When - 旧实例迟到的确认和失败回写抵达。
                mapper.markSent(due.getId(), due.getNextAttemptAt());
                mapper.defer(due.getId(), due.getNextAttemptAt(), "SEND_FAILED");
                // Then - 新租约仍然有效。
                assertEquals(1, jdbc.queryForObject("select count(*) from activity_admission_outbox where sent_at is null and attempts = 0", Integer.class));
                mapper.defer(due.getId(), due.getClaimUntil(), "ACK_UNCERTAIN");
                var backlog = mapper.backlog();
                assertEquals(1L, backlog.pending());
                assertEquals(true, backlog.oldestSeconds() >= 0);
                assertEquals("ACK_UNCERTAIN", jdbc.queryForObject(
                        "select last_error from activity_admission_outbox where id = 'claim-test'", String.class));
                assertEquals(1, jdbc.queryForObject(
                        "select attempts from activity_admission_outbox where id = 'claim-test'", Integer.class));
            }

            // Given / When - 回执随事务回滚，死信随后可以占用相同事件主键。
            try (var session = sessions.openSession(false)) {
                session.getMapper(ActivityAdmissionReceiptMapper.class).insert("receipt-test", "CONSUMED", null, null, null, null);
                session.rollback();
            }
            try (var session = sessions.openSession(true)) {
                var receipts = session.getMapper(ActivityAdmissionReceiptMapper.class);
                receipts.insert("receipt-test", "DEAD", "{}", 1, "s1", "test-token");
                // Then - 真实 SQL 能读到补偿参数并持久化完成时间。
                assertEquals("test-token", receipts.findPending().getFirst().reservationToken());
                receipts.complete("receipt-test");
                assertEquals(0, receipts.findPending().size());
                assertEquals("receipt-test", receipts.findDead().getFirst().get("id"));
            }
        } finally {
            server.execute("drop database " + schema);
        }
    }

    private void dispatch(org.apache.ibatis.session.SqlSessionFactory sessions, KafkaProducer producer,
                          CyclicBarrier bothRead) {
        try (var session = sessions.openSession(true)) {
            var actual = session.getMapper(ActivityAdmissionOutboxMapper.class);
            var mapper = mock(ActivityAdmissionOutboxMapper.class, delegatesTo(actual));
            doAnswer(invocation -> {
                var due = actual.findDue();
                assertEquals(1, due.size());
                bothRead.await(5, TimeUnit.SECONDS);
                return due;
            }).when(mapper).findDue();
            var service = new ActivityAdmissionOutboxService();
            ReflectionTestUtils.setField(service, "mapper", mapper);
            ReflectionTestUtils.setField(service, "producer", producer);
            service.dispatchPending();
        }
    }

    private UnpooledDataSource dataSource(String schema, String user, String password) {
        return new UnpooledDataSource("com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://127.0.0.1:3306/" + schema + "?connectTimeout=3000&socketTimeout=5000",
                user, password);
    }
}
