package com.haufe.beercatalogue.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class HttpLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);
    private static final int MAX_BODY_LENGTH = 2_000;
    private static final List<String> TEXTUAL_CONTENT_TYPES = List.of(
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.TEXT_HTML_VALUE
    );

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        final var wrappedRequest = new ContentCachingRequestWrapper(request, MAX_BODY_LENGTH);
        final var wrappedResponse = new ContentCachingResponseWrapper(response);
        final var startTime = System.currentTimeMillis();

        log.info(
                "Incoming request: method={} uri={} query={} contentType={}",
                wrappedRequest.getMethod(),
                wrappedRequest.getRequestURI(),
                wrappedRequest.getQueryString(),
                wrappedRequest.getContentType()
        );

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            final var durationMs = System.currentTimeMillis() - startTime;

            log.info(
                    "Completed request: method={} uri={} status={} durationMs={} requestBody={} responseBody={}",
                    wrappedRequest.getMethod(),
                    wrappedRequest.getRequestURI(),
                    wrappedResponse.getStatus(),
                    durationMs,
                    extractRequestBody(wrappedRequest),
                    extractResponseBody(wrappedResponse)
            );

            wrappedResponse.copyBodyToResponse();
        }
    }

    private String extractRequestBody(final ContentCachingRequestWrapper request) {
        if (!isTextualContentType(request.getContentType()) || request.getContentAsByteArray().length == 0) {
            return "<not-logged>";
        }

        return formatBody(request.getContentAsByteArray());
    }

    private String extractResponseBody(final ContentCachingResponseWrapper response) {
        if (!isTextualContentType(response.getContentType()) || response.getContentAsByteArray().length == 0) {
            return "<not-logged>";
        }

        return formatBody(response.getContentAsByteArray());
    }

    private boolean isTextualContentType(final String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }

        return TEXTUAL_CONTENT_TYPES.stream().anyMatch(contentType::startsWith);
    }

    private String formatBody(final byte[] content) {
        final var body = new String(content, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();

        if (body.length() <= MAX_BODY_LENGTH) {
            return body;
        }

        return body.substring(0, MAX_BODY_LENGTH) + "...";
    }
}
