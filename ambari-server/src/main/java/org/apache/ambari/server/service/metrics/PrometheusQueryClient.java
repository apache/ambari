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
package org.apache.ambari.server.service.metrics;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.entities.DatasourceEntity;
import org.apache.ambari.server.security.InternalSSLSocketFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PrometheusQueryClient {
  private static final int DEFAULT_TIMEOUT_MILLIS = 10_000;
  private static final int MAX_TIMEOUT_MILLIS = 60_000;
  private static final int MAX_RESPONSE_BYTES = 16 * 1024 * 1024;
  private static final int MAX_REQUEST_BYTES = 8 * 1024 * 1024;
  private static final Set<String> BLOCKED_HEADERS = Set.of(
      "connection", "content-length", "expect", "host", "transfer-encoding", "upgrade");

  private final DatasourceService datasourceService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient;
  private final HttpClient insecureHttpClient;

  @Inject
  public PrometheusQueryClient(DatasourceService datasourceService) {
    this.datasourceService = datasourceService;
    httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(DEFAULT_TIMEOUT_MILLIS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    insecureHttpClient = createInsecureHttpClient();
  }

  public JsonNode get(long datasourceId, String endpoint, Map<String, List<String>> parameters)
      throws AmbariException, AuthorizationException, PrometheusClientException {
    return execute(datasourceId, endpoint, parameters, "GET", null, null, true);
  }

  public JsonNode proxy(long datasourceId, String endpoint, Map<String, List<String>> parameters,
      String method, String body, String contentType)
      throws AmbariException, AuthorizationException, PrometheusClientException {
    return execute(datasourceId, endpoint, parameters, method, body, contentType, false);
  }

  public JsonNode testConnection(long datasourceId)
      throws AmbariException, AuthorizationException, PrometheusClientException {
    DatasourceEntity datasource = datasourceService.requireQueryable(datasourceId);
    String pluginType = datasource.getPluginType().toLowerCase(Locale.ROOT);
    switch (pluginType) {
      case "prometheus":
        return execute(datasourceId, "api/v1/status/buildinfo", Map.of(),
            "GET", null, null, true);
      case "elasticsearch":
        return proxy(datasourceId, "_cluster/health", Map.of(), "GET", null, null);
      case "loki":
        return proxy(datasourceId, "loki/api/v1/labels", Map.of(), "GET", null, null);
      case "jaeger":
        return proxy(datasourceId, "api/services", Map.of(), "GET", null, null);
      case "tdengine":
        return proxy(datasourceId, "rest/sql", Map.of(), "POST",
            "SELECT SERVER_VERSION()", "text/plain");
      default:
        return proxy(datasourceId, "", Map.of(), "GET", null, null);
    }
  }

  private JsonNode execute(long datasourceId, String endpoint, Map<String, List<String>> parameters,
      String method, String body, String contentType, boolean requirePrometheus)
      throws AmbariException, AuthorizationException, PrometheusClientException {
    byte[] requestBody = null;
    if ("POST".equals(method)) {
      requestBody = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
      if (requestBody.length > MAX_REQUEST_BYTES) {
        throw new IllegalArgumentException("Datasource proxy request exceeds the 8 MiB limit");
      }
    } else if (!"GET".equals(method)) {
      throw new IllegalArgumentException("Datasource proxy supports only GET and POST");
    }

    DatasourceEntity datasource = datasourceService.requireQueryable(datasourceId);
    if (requirePrometheus && !"prometheus".equalsIgnoreCase(datasource.getPluginType())
        && !"prometheus".equalsIgnoreCase(datasource.getCategory())) {
      throw new IllegalArgumentException("Datasource is not a Prometheus datasource");
    }

    JsonNode http = datasourceService.resolveHttp(datasource);
    JsonNode auth = datasourceService.resolveAuth(datasource);
    String baseUrl = http.path("url").asText(null);
    if ((baseUrl == null || baseUrl.isBlank()) && http.path("urls").isArray()
        && !http.path("urls").isEmpty()) {
      baseUrl = http.path("urls").get(0).asText(null);
    }
    URI target = buildUri(baseUrl, endpoint, parameters);
    int timeoutMillis = boundedTimeout(http.path("timeout").asInt(DEFAULT_TIMEOUT_MILLIS));

    HttpRequest.Builder request = HttpRequest.newBuilder(target)
        .timeout(Duration.ofMillis(timeoutMillis))
        .header("Accept", "application/json");
    addHeaders(request, http.path("headers"));
    addBasicAuth(request, auth);
    if ("GET".equals(method)) {
      request.GET();
    } else {
      request.header("Content-Type", contentType == null || contentType.isBlank()
          ? "application/json" : contentType);
      request.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody));
    }

    try {
      HttpClient client = http.path("tls").path("skip_tls_verify").asBoolean(false)
          ? insecureHttpClient : httpClient;
      HttpResponse<InputStream> response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream stream = response.body()) {
        byte[] responseBody = stream.readNBytes(MAX_RESPONSE_BYTES + 1);
        if (responseBody.length > MAX_RESPONSE_BYTES) {
          throw new PrometheusClientException("Prometheus response exceeded the 16 MiB limit");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          throw new PrometheusClientException(
              "Prometheus returned HTTP " + response.statusCode(), response.statusCode());
        }
        try {
          return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
          throw new PrometheusClientException("Prometheus returned invalid JSON", e);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PrometheusClientException("Prometheus request was interrupted", e);
    } catch (IOException e) {
      throw new PrometheusClientException("Prometheus request failed", e);
    }
  }

  URI buildUri(String rawBaseUrl, String endpoint, Map<String, List<String>> parameters) {
    if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
      throw new IllegalArgumentException("Datasource HTTP URL is required");
    }
    try {
      URI base = new URI(rawBaseUrl.trim());
      String scheme = base.getScheme() == null ? "" : base.getScheme().toLowerCase(Locale.ROOT);
      if (!("http".equals(scheme) || "https".equals(scheme)) || base.getHost() == null) {
        throw new IllegalArgumentException("Datasource URL must use HTTP or HTTPS and include a host");
      }
      if (base.getRawUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null) {
        throw new IllegalArgumentException("Datasource URL cannot contain user info, a query, or a fragment");
      }
      String basePath = base.getRawPath() == null ? "" : base.getRawPath();
      if (!basePath.endsWith("/")) {
        basePath += "/";
      }
      String relativeEndpoint = endpoint.startsWith("/") ? endpoint.substring(1) : endpoint;
      URI relative = new URI(relativeEndpoint);
      if (relative.isAbsolute() || relative.getRawAuthority() != null || relative.getRawQuery() != null
          || relative.getRawFragment() != null || relativeEndpoint.contains("..")) {
        throw new IllegalArgumentException("Datasource proxy path is invalid");
      }
      URI resolved = new URI(base.getScheme(), base.getRawAuthority(), basePath, null, null)
          .resolve(relativeEndpoint);
      if (!resolved.getRawPath().startsWith(basePath)) {
        throw new IllegalArgumentException("Datasource proxy path is outside the configured base URL");
      }
      String query = encodeParameters(parameters);
      return new URI(resolved.getScheme(), resolved.getRawAuthority(), resolved.getRawPath(), query, null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Datasource URL is invalid", e);
    }
  }

  private String encodeParameters(Map<String, List<String>> parameters) {
    List<String> encoded = new ArrayList<>();
    parameters.forEach((name, values) -> {
      if (name == null || values == null) {
        return;
      }
      for (String value : values) {
        if (value != null) {
          encoded.add(URLEncoder.encode(name, StandardCharsets.UTF_8)
              + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
      }
    });
    return String.join("&", encoded);
  }

  private void addHeaders(HttpRequest.Builder request, JsonNode headers) {
    if (headers.isObject()) {
      headers.fields().forEachRemaining(header -> addHeader(request, header.getKey(), header.getValue().asText(null)));
      return;
    }
    if (!headers.isArray()) {
      return;
    }
    for (JsonNode header : headers) {
      String name = header.path("key").asText(header.path("name").asText(""));
      String value = header.path("value").asText(header.path("val").asText(null));
      addHeader(request, name, value);
    }
  }

  private void addHeader(HttpRequest.Builder request, String rawName, String value) {
    String name = rawName == null ? "" : rawName.trim();
    if (!name.isEmpty() && value != null && !BLOCKED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
      request.header(name, value);
    }
  }

  private void addBasicAuth(HttpRequest.Builder request, JsonNode auth) {
    String user = auth.path("basic_auth_user").asText(null);
    String password = auth.path("basic_auth_password").asText(null);
    if (user == null || user.isEmpty()) {
      return;
    }
    String value = Base64.getEncoder().encodeToString(
        (user + ":" + (password == null ? "" : password)).getBytes(StandardCharsets.UTF_8));
    request.setHeader("Authorization", "Basic " + value);
  }

  private int boundedTimeout(int requested) {
    if (requested <= 0) {
      return DEFAULT_TIMEOUT_MILLIS;
    }
    return Math.min(requested, MAX_TIMEOUT_MILLIS);
  }

  private HttpClient createInsecureHttpClient() {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      TrustManager[] trustManagers = {new InternalSSLSocketFactory.LenientTrustManager()};
      sslContext.init(null, trustManagers, new SecureRandom());
      SSLParameters sslParameters = new SSLParameters();
      sslParameters.setEndpointIdentificationAlgorithm("");
      return HttpClient.newBuilder()
          .connectTimeout(Duration.ofMillis(DEFAULT_TIMEOUT_MILLIS))
          .followRedirects(HttpClient.Redirect.NEVER)
          .version(HttpClient.Version.HTTP_1_1)
          .sslContext(sslContext)
          .sslParameters(sslParameters)
          .build();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Unable to initialize datasource TLS support", e);
    }
  }

  public static Map<String, List<String>> parameters(Object... nameValues) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    for (int i = 0; i + 1 < nameValues.length; i += 2) {
      if (nameValues[i + 1] != null) {
        result.put(String.valueOf(nameValues[i]), List.of(String.valueOf(nameValues[i + 1])));
      }
    }
    return result;
  }
}
