package org.apache.ambari.server.api.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * Filter to work around the original limitation of the REST API implementation of Amabri,
 * where the Content-Type is always text/plain, despite the accepted/returned JSON content.
 * When the request uses application/json as Content-Type, Ambari fails.
 *
 * This workaround is replacing application/json Content-Type to text/plain in requests, thus
 * preventing the failure.
 */
public class ContentTypeOverrideFilter implements Filter {

    private class ContentTypeOverrideRequestWrapper extends HttpServletRequestWrapper {

        public ContentTypeOverrideRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            Enumeration<String> headerValues = super.getHeaders(name);
            if (HttpHeaders.CONTENT_TYPE.equals(name)) {
                Set<String> newContentTypeValues = new HashSet<>();
                while (headerValues.hasMoreElements()) {
                    String value = headerValues.nextElement();
                    if(value != null && value.startsWith(MediaType.APPLICATION_JSON)) {
                        newContentTypeValues.add(value.replace(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
                    } else {
                        newContentTypeValues.add(value);
                    }
                }
                return Collections.enumeration(newContentTypeValues);
            }
            return headerValues;
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.CONTENT_TYPE.equals(name)) {
                String header = super.getHeader(name);
                if (header != null && header.startsWith(MediaType.APPLICATION_JSON)) {
                    return header.replace(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
                }
            }
            return super.getHeader(name);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            String contentType = httpServletRequest.getContentType();

            if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON)) {
                ContentTypeOverrideRequestWrapper requestWrapper = new ContentTypeOverrideRequestWrapper(httpServletRequest);
                chain.doFilter(requestWrapper, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
