package com.chinahitech.shop.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Elasticsearch 只作为可重建搜索索引；未配置时应用仍可使用 MySQL 查询。 */
@Configuration
public class ElasticsearchConfig {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true")
    public RestHighLevelClient elasticsearchClient(
            @Value("${app.elasticsearch.host}") String host,
            @Value("${app.elasticsearch.port}") int port,
            @Value("${app.elasticsearch.scheme:http}") String scheme,
            @Value("${app.elasticsearch.username:}") String username,
            @Value("${app.elasticsearch.password:}") String password) {
        org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme))
                .setRequestConfigCallback(config -> config.setConnectTimeout(1000)
                        .setSocketTimeout(3000).setConnectionRequestTimeout(500));
        if (!username.isBlank()) {
            org.apache.http.impl.client.BasicCredentialsProvider credentials = new org.apache.http.impl.client.BasicCredentialsProvider();
            credentials.setCredentials(org.apache.http.auth.AuthScope.ANY,
                    new org.apache.http.auth.UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(config -> config.setDefaultCredentialsProvider(credentials));
        }
        return new RestHighLevelClient(builder);
    }
}
