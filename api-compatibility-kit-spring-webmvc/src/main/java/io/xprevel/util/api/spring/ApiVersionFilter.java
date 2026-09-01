package io.xprevel.util.api.spring;

import io.xprevel.util.api.VersionResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Consumer;

public final class ApiVersionFilter<E extends Enum<E>> extends OncePerRequestFilter {
    private final VersionResolver<E, HttpServletRequest> resolver;
    private final Consumer<E> versionConsumer;

    public ApiVersionFilter(VersionResolver<E, HttpServletRequest> resolver, Consumer<E> versionConsumer) {
        this.resolver = resolver;
        this.versionConsumer = versionConsumer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        E apiVersion = resolver.resolve(request);
        versionConsumer.accept(apiVersion);
        filterChain.doFilter(request, response);
    }
}