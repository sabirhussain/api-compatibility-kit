package io.xprevel.util.api;

@FunctionalInterface
public interface VersionResolver<E extends Enum<E>, S> {
    E resolve(S source);
}
