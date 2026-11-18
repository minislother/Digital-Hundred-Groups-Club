package com.chinahitech.shop.service;

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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** 隔离数据库验证真实分页 SQL、重复索引迁移与深翻页执行计划。 */
@EnabledIfEnvironmentVariable(named = "BAITUAN_RUN_MYSQL_INTEGRATION", matches = "true")
@DisplayName("列表分页真实数据库验证")
class ListPaginationIntegrationTest {
    private UnpooledDataSource source(String schema) {
        return new UnpooledDataSource("com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://127.0.0.1:3306/" + schema + "?connectTimeout=3000&socketTimeout=5000",
                System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
    }

    @Test
    @DisplayName("分页SQL - 所有新列表执行成功且游标使用索引")
    void testPagination_RealDatabase_ConsistentSlicesAndIndexes() throws Exception {
        // Given
        String schema = "baituan_page_test_" + UUID.randomUUID().toString().replace("-", "");
        var server = new JdbcTemplate(source(""));
        server.execute("create database " + schema);
        try {
            var database = source(schema);
            new ResourceDatabasePopulator(new ClassPathResource("db/flyway/V1__initial_schema.sql"),
                    new ClassPathResource("db/flyway/V7__list_pagination_indexes.sql"),
                    new ClassPathResource("db/flyway/V7__list_pagination_indexes.sql")).execute(database);
            var config = new Configuration(new Environment("pagination", new JdbcTransactionFactory(), database));
            config.setMapUnderscoreToCamelCase(true);
            for (String name : List.of("Activity", "Group", "IndividualActivity", "IndividualGroup", "Inspection", "StuApp")) {
                String path = "mapper/" + name + "Mapper.xml";
                try (var xml = new ClassPathResource(path).getInputStream()) {
                    new XMLMapperBuilder(xml, config, path, config.getSqlFragments()).parse();
                }
            }
            var jdbc = new JdbcTemplate(database);
            jdbc.update("insert into activity(id,name,is_accepted) values(1,'page',true)");
            var inserts = new ArrayList<Object[]>();
            for (int id = 1; id <= 2000; id++) inserts.add(new Object[]{id, "s" + id});
            jdbc.batchUpdate("insert into individual_activity(id,activity_id,user_id,status) values(?,1,?,1)", inserts);
            var params = new HashMap<String,Object>();
            params.put("activityId",1); params.put("studentId","s100"); params.put("groupId",1);
            params.put("groupName","page"); params.put("searchInfo",null); params.put("stunumber","s100");
            params.put("year",2026); params.put("offset",0); params.put("limit",10);

            // When / Then - Each generated statement is actually executed against MySQL.
            try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
                for (String id : config.getMappedStatementNames().stream().filter(id -> id.contains(".")
                        && (id.endsWith("Page") || id.endsWith("Count"))).toList()) {
                    if (id.endsWith("Page")) assertTrue(session.selectList(id, params).size() <= 10, id);
                    else assertNotNull(session.selectOne(id, params), id);
                }
                params.put("beforeId",null); params.put("limit",11);
                var rows = session.selectList("com.chinahitech.shop.mapper.IndividualActivityMapper.studentSlice", params);
                assertEquals(1, rows.size());
                jdbc.execute("analyze table individual_activity");
                var plan = jdbc.queryForList("explain format=traditional select * from individual_activity where user_id='s100' and id < 2000 order by id desc limit 11");
                assertTrue(String.valueOf(plan.getFirst().get("possible_keys")).contains("idx_ia_user_id"));
                assertTrue(((Number) plan.getFirst().get("rows")).longValue() < 100);
                System.out.println("PAGINATION_EXPLAIN " + plan);
            }
        } finally {
            server.execute("drop database " + schema);
        }
    }
}
