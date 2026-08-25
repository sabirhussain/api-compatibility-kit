package io.xprevel.util.api.spring.auto.config;

import io.xprevel.util.api.ApiVersion;
import io.xprevel.util.api.ApiVersionContext;
import io.xprevel.util.api.ApiVersionResolver;
import io.xprevel.util.api.spring.ApiVersionFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.annotation.RequestScope;

@AutoConfiguration
@EnableConfigurationProperties(ApiCompatibilityProperties.class)
public class ApiCompatibilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ApiVersionResolver.class)
    ApiVersionResolver<ApiVersion> apiVersionResolver(ApiCompatibilityProperties properties) {
        ApiVersion version = ApiVersion.valueOf(properties.getDefaultVersion());
        return new ApiVersionResolver<>(version);
    }

    @Bean
    @RequestScope
    @ConditionalOnMissingBean
    <E extends Enum<E>> ApiVersionContext<E> apiVersionContext() {
        return new ApiVersionContext<>();
    }

    @Bean
    @ConditionalOnMissingBean
    <E extends Enum<E>> ApiVersionFilter<E> apiVersionFilter(ApiVersionResolver<E> resolver, ApiVersionContext<E> context, ApiCompatibilityProperties properties) {
        return new ApiVersionFilter<>(resolver, context, properties.getVersionHeader());
    }
}
