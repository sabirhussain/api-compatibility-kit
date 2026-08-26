package io.xprevel.util.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ApiVersionResolverTest {
    private final ApiVersionResolver<ApiVersion> resolver = new ApiVersionResolver<>(ApiVersion.V2026_02_01);

    //TODO: Paramterized test - null, "", "  "
    @Test
    void shouldGetDefaultVersionWhenNotProvided() {
        ApiVersion version = resolver.resolve(null);
        Assertions.assertSame(ApiVersion.V2026_02_01, version);
    }

    @Test
    void shouldGetDesiredVersionWhenProvided() {
        ApiVersion version = resolver.resolve(ApiVersion.V2026_08_01.name());
        Assertions.assertSame(ApiVersion.V2026_08_01, version);
    }

    @Test
    void shouldThrowExceptionWhenWrongVersionProvided() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> resolver.resolve("INVALID_VERSION"));
        Assertions.assertTrue(exception.getMessage().contains("INVALID_VERSION"));
    }
}
