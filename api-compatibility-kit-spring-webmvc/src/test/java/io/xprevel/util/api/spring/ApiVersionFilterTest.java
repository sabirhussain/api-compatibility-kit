package io.xprevel.util.api.spring;

import io.xprevel.util.api.ApiVersion;
import io.xprevel.util.api.ApiVersionContext;
import io.xprevel.util.api.VersionResolver;
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

    private final VersionResolver<ApiVersion, HttpServletRequest> resolver = (req) -> ApiVersion.valueOf(req.getHeader(Mockito.anyString()));

    private ApiVersionFilter<ApiVersion> filter;

    private static Stream<ApiVersion> getApiVersions() {
        return Stream.of(ApiVersion.values());
    }

    @BeforeEach
    void init() {
        filter = new ApiVersionFilter<>(resolver, context::setVersion);
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
        ApiVersionFilter<ApiVersion> versionFilter = new ApiVersionFilter<>(req -> req.getHeader(Mockito.anyString()) == null ? ApiVersion.V2026_02_01 : ApiVersion.V2026_08_01, context::setVersion);
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
