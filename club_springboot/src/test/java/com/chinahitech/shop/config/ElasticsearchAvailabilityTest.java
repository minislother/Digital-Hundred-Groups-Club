package com.chinahitech.shop.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可选的真实 ES 冒烟测试。未配置或 ES 未启动时跳过，不阻塞没有搜索服务的本地开发。
 * CI/生产验证时设置 ELASTICSEARCH_SMOKE_URL=http://localhost:9200。
 */
@DisplayName("Elasticsearch连接冒烟测试")
class ElasticsearchAvailabilityTest {
    @Test
    @DisplayName("ES服务 - 能返回集群信息")
    void testElasticsearchAvailable() {
        String url = System.getenv("ELASTICSEARCH_SMOKE_URL");
        Assumptions.assumeTrue(url != null && !url.trim().isEmpty(), "未设置ELASTICSEARCH_SMOKE_URL，跳过真实ES测试");
        try {
            Map<?, ?> info = new RestTemplate().getForObject(url, Map.class);
            assertTrue(info != null && info.containsKey("cluster_name"), "ES响应缺少cluster_name");
        } catch (Exception e) {
            throw new AssertionError("Elasticsearch不可用: " + url, e);
        }
    }
}
