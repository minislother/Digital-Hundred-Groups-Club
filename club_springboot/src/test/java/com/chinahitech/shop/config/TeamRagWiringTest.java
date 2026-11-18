package com.chinahitech.shop.config;

import com.chinahitech.shop.service.TeamRagClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Boot4知识库适配装配测试")
class TeamRagWiringTest {
    @Test @DisplayName("Jackson兼容模块 - 可装配真实TeamRag客户端")
    void testClient_JacksonCompatibility_WiresWithoutManualMapper() {
        // Given - 使用实际Boot4兼容自动配置，不以测试Mock代替序列化依赖。
        var runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Jackson2AutoConfiguration.class))
                .withUserConfiguration(TeamRagProperties.class, TeamRagClient.class);
        // When / Then
        runner.run(context -> assertThat(context).hasNotFailed().hasSingleBean(TeamRagClient.class));
    }
}
