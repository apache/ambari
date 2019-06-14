package org.apache.ambari.server.api.util;

import java.io.IOException;
import java.lang.reflect.Method;
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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Filter to work around the original limitation of the REST API implementation of Amabri,
 * where the Content-Type is always text/plain, despite the accepted/returned JSON content.
 * When the request uses application/json as Content-Type, Ambari fails.
 *
 * This workaround is replacing application/json Content-Type to text/plain in requests, thus
 * preventing the failure.
 *
 * Furthermore the response is also tweaked by changing the Content-Type to application/json
 * when the original Content-Type of the request was application/json.
 */
public class ContentTypeOverrideFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ContentTypeOverrideFilter.class);

    private final Set<String> excludedUrls = new HashSet<>();

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

    private class ContentTypeOverrideResponseWrapper extends HttpServletResponseWrapper {

        public ContentTypeOverrideResponseWrapper(HttpServletResponse response) {
            super(response);
            super.setContentType(MediaType.APPLICATION_JSON);
        }

        @Override
        public void setHeader(String name, String value) {
            if (!HttpHeaders.CONTENT_TYPE.equals(name)) {
                super.setHeader(name, value);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if (!HttpHeaders.CONTENT_TYPE.equals(name)) {
                super.addHeader(name, value);
            }
        }

        @Override
        public void setContentType(String type) {
            // Ignore changing the content type
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            String contentType = httpServletRequest.getContentType();

            if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON) && !excludedUrls.contains(httpServletRequest.getPathInfo())) {
                ContentTypeOverrideRequestWrapper requestWrapper = new ContentTypeOverrideRequestWrapper(httpServletRequest);
                ContentTypeOverrideResponseWrapper responseWrapper = new ContentTypeOverrideResponseWrapper((HttpServletResponse) response);

                chain.doFilter(requestWrapper, responseWrapper);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            ClassPath classPath = ClassPath.from(ClassLoader.getSystemClassLoader());
            ImmutableSet<ClassPath.ClassInfo> classes = classPath.getTopLevelClassesRecursive("org.apache.ambari.server.api");

            restart:
            for (ClassPath.ClassInfo classInfo: classes) {
                Class<?> clazz = classInfo.load();
                if (clazz.isAnnotationPresent(Path.class)) {
                    Path path = clazz.getAnnotation(Path.class);
                    for (Method method : clazz.getMethods()) {
                        if (method.isAnnotationPresent(Consumes.class)) {
                            Consumes consumesAnnotation = method.getAnnotation(Consumes.class);
                            for (String consume : consumesAnnotation.value()) {
                                if (MediaType.APPLICATION_JSON.equals(consume)) {
                                    excludedUrls.add(path.value());
                                    continue restart;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to discover URLs that are excluded from Content-Type override. Falling back to pre-defined list of exluded URLs.", e);

            /* Do not fail here, but fallback to manual definition of excluded endpoints. */
            excludedUrls.add("/bootstrap");
        }
    }

    @Override
    public void destroy() {
    }
}
