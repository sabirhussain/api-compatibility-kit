package io.xprevel.util.api.spring;

import io.xprevel.util.api.ApiVersionContext;
import io.xprevel.util.api.ApiVersionResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiVersionFilter<E extends Enum<E>> extends OncePerRequestFilter {
    private final String headerName;
    private final ApiVersionResolver<E> resolver;
    private final ApiVersionContext<E> context;

    public ApiVersionFilter(ApiVersionResolver<E> resolver, ApiVersionContext<E> context, String headerName) {
        this.resolver = resolver;
        this.context = context;
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        E apiVersion = resolver.resolve(request.getHeader(headerName));
        context.setVersion(apiVersion);
        filterChain.doFilter(request, response);
    }
}