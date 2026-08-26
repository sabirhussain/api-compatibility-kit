package io.xprevel.util.api;

public class ApiVersionResolver<E extends Enum<E>> {
    private final E defaultApiVersion;

    public ApiVersionResolver(E defaultApiVersion) {
        this.defaultApiVersion = defaultApiVersion;
    }

    public E resolve(String version) {
        if (version == null || version.isBlank()) {
            return defaultApiVersion;
        }

        return Enum.valueOf(defaultApiVersion.getDeclaringClass(), version);
    }
}
