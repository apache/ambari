/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MethodOverrideFilter implements Filter {
  private static final String HEADER_NAME = "X-Http-Method-Override";
  private static final Set<String> ALLOWED_METHODS = Set.of("GET");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpServletRequest = (HttpServletRequest) request;

      String method = httpServletRequest.getHeader(HEADER_NAME);
      if (method != null) {
        if (ALLOWED_METHODS.contains(method.toUpperCase())) {
          final HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
          byte[] body = httpServletRequest.getInputStream().readAllBytes();
          String bodyQuery = getBodyQuery(body);
          HttpServletRequestWrapper requestWrapper = new MethodOverrideRequestWrapper(
              httpServletRequest, httpMethod, bodyQuery, body);

          chain.doFilter(requestWrapper, response);
          return;
        } else {
          HttpServletResponse httpResponse = (HttpServletResponse) response;
          httpResponse.sendError(400, "Incorrect HTTP method for override: "+ method + ". Allowed values: "+ ALLOWED_METHODS);
          return;
        }
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {

  }

  private String getBodyQuery(byte[] body) throws IOException {
    if (body.length == 0) {
      return null;
    }

    try {
      JsonNode query = OBJECT_MAPPER.readTree(body).path("RequestInfo").path("query");
      return query.isTextual() && !query.textValue().isEmpty() ? query.textValue() : null;
    } catch (JsonProcessingException ignored) {
      return null;
    }
  }

  private static final class MethodOverrideRequestWrapper extends HttpServletRequestWrapper {
    private final HttpMethod method;
    private final String queryString;
    private final byte[] body;

    private MethodOverrideRequestWrapper(HttpServletRequest request, HttpMethod method, String bodyQuery,
        byte[] body) {
      super(request);
      this.method = method;
      this.body = body;
      String originalQuery = request.getQueryString();
      if (originalQuery == null || originalQuery.isEmpty()) {
        queryString = bodyQuery;
      } else if (bodyQuery == null || bodyQuery.isEmpty()) {
        queryString = originalQuery;
      } else {
        queryString = originalQuery + "&" + bodyQuery;
      }
    }

    @Override
    public String getMethod() {
      return method.toString();
    }

    @Override
    public String getQueryString() {
      return queryString;
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream input = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public int read() {
          return input.read();
        }

        @Override
        public boolean isFinished() {
          return input.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
          throw new UnsupportedOperationException("Asynchronous reads are not supported");
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      String encoding = getCharacterEncoding();
      Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
  }
}
