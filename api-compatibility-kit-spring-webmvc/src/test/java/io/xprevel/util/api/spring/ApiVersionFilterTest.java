package io.xprevel.util.api.spring;

import io.xprevel.util.api.ApiVersion;
import io.xprevel.util.api.ApiVersionContext;
import io.xprevel.util.api.ApiVersionResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.stream.Stream;

class ApiVersionFilterTest {
    private final ApiVersionContext<ApiVersion> context = mockApiVersionContext();

    private final ApiVersionResolver<ApiVersion> resolver = new ApiVersionResolver<>(ApiVersion.V2026_02_01);

    private ApiVersionFilter<ApiVersion> filter;

    private static Stream<ApiVersion> getApiVersions() {
        return Stream.of(ApiVersion.values());
    }

    @BeforeEach
    void init() {
        filter = new ApiVersionFilter<>(resolver, context, "X-API-Version");
    }

    @ParameterizedTest
    @MethodSource("getApiVersions")
    void shouldSetApiVersionInContextWhenProvided(ApiVersion version) throws Exception {
        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(Mockito.anyString())).thenReturn(version.name());

        filter.doFilterInternal(request, null, chain);
        Mockito.verify(context).setVersion(version);
    }

    @Test
    void shouldSetDefaultApiVersionInContextWhenNoneProvided() throws Exception {
        ApiVersionFilter<ApiVersion> versionFilter = new ApiVersionFilter<>(new ApiVersionResolver<>(ApiVersion.V2026_02_01), context, "X-API-Version");
        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(Mockito.anyString())).thenReturn(null);

        versionFilter.doFilterInternal(request, null, chain);
        Mockito.verify(context).setVersion(ApiVersion.V2026_02_01);
    }

    @Test
    void shouldThrowExceptionWhenWrongVersionProvided() throws ServletException, IOException {
        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(Mockito.anyString())).thenReturn("INVALID_VERSION");

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> filter.doFilterInternal(request, null, chain));
        Assertions.assertTrue(exception.getMessage().contains("INVALID_VERSION"));
    }

    @SuppressWarnings("unchecked")
    private ApiVersionContext<ApiVersion> mockApiVersionContext() {
        return Mockito.mock(ApiVersionContext.class);
    }
}
