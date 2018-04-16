/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.core.timeline.sink;

import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_COMMIT_INTERVAL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.Collection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration;
import org.apache.ambari.metrics.core.timeline.source.InternalSourceProvider;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

public class HttpSinkProvider implements ExternalSinkProvider {
  private static final Log LOG = LogFactory.getLog(HttpSinkProvider.class);
  TimelineMetricConfiguration conf = TimelineMetricConfiguration.getInstance();

  private String connectUrl;
  private SSLSocketFactory sslSocketFactory;
  protected static ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    mapper.getSerializationConfig()
      .withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
  }

  public HttpSinkProvider() {
    Configuration config;
    try {
      config = conf.getMetricsConf();
    } catch (Exception e) {
      throw new ExceptionInInitializerError("Unable to read configuration for sink.");
    }
    String protocol = config.get("timeline.metrics.service.external.http.sink.protocol", "http");
    String host = config.get("timeline.metrics.service.external.http.sink.host", "localhost");
    String port = config.get("timeline.metrics.service.external.http.sink.port", "6189");

    if (protocol.contains("https")) {
      loadTruststore(
        config.getTrimmed("timeline.metrics.service.external.http.sink.truststore.path"),
        config.getTrimmed("timeline.metrics.service.external.http.sink.truststore.type"),
        config.getTrimmed("timeline.metrics.service.external.http.sink.truststore.password")
      );
    }

    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(protocol);
    uriBuilder.setHost(host);
    uriBuilder.setPort(Integer.parseInt(port));
    connectUrl = uriBuilder.toString();
  }

  @Override
  public ExternalMetricsSink getExternalMetricsSink(InternalSourceProvider.SOURCE_NAME sourceName) {
    return new DefaultHttpMetricsSink();
  }

  protected HttpURLConnection getConnection(String spec) throws IOException {
    return (HttpURLConnection) new URL(spec).openConnection();
  }

  // Get an ssl connection
  protected HttpsURLConnection getSSLConnection(String spec)
    throws IOException, IllegalStateException {

    HttpsURLConnection connection = (HttpsURLConnection) (new URL(spec).openConnection());
    connection.setSSLSocketFactory(sslSocketFactory);
    return connection;
  }

  protected void loadTruststore(String trustStorePath, String trustStoreType,
                                String trustStorePassword) {
    if (sslSocketFactory == null) {
      if (trustStorePath == null || trustStorePassword == null) {
        String msg = "Can't load TrustStore. Truststore path or password is not set.";
        LOG.error(msg);
        throw new IllegalStateException(msg);
      }
      FileInputStream in = null;
      try {
        in = new FileInputStream(new File(trustStorePath));
        KeyStore store = KeyStore.getInstance(trustStoreType == null ?
          KeyStore.getDefaultType() : trustStoreType);
        store.load(in, trustStorePassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory
          .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(store);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);
        sslSocketFactory = context.getSocketFactory();
      } catch (Exception e) {
        LOG.error("Unable to load TrustStore", e);
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            LOG.error("Unable to load TrustStore", e);
          }
        }
      }
    }
  }

  class DefaultHttpMetricsSink implements ExternalMetricsSink {

    @Override
    public int getSinkTimeOutSeconds() {
      try {
        return conf.getMetricsConf().getInt("timeline.metrics.external.sink.http.timeout.seconds", 10);
      } catch (Exception e) {
        return 10;
      }
    }

    @Override
    public int getFlushSeconds() {
      try {
        return conf.getMetricsConf().getInt(TIMELINE_METRICS_CACHE_COMMIT_INTERVAL, 3);
      } catch (Exception e) {
        LOG.warn("Cannot read cache commit interval.");
      }
      return 3;
    }

    /**
     * Cleans up and closes an input stream
     * see http://docs.oracle.com/javase/6/docs/technotes/guides/net/http-keepalive.html
     * @param is the InputStream to clean up
     * @return string read from the InputStream
     * @throws IOException
     */
    protected String cleanupInputStream(InputStream is) throws IOException {
      StringBuilder sb = new StringBuilder();
      if (is != null) {
        try (
          InputStreamReader isr = new InputStreamReader(is);
          BufferedReader br = new BufferedReader(isr)
        ) {
          // read the response body
          String line;
          while ((line = br.readLine()) != null) {
            if (LOG.isDebugEnabled()) {
              sb.append(line);
            }
          }
        } finally {
          is.close();
        }
      }
      return sb.toString();
    }

    @Override
    public void sinkMetricData(Collection<TimelineMetrics> metrics) {
      HttpURLConnection connection = null;
      try {
        connection = connectUrl.startsWith("https") ? getSSLConnection(connectUrl) : getConnection(connectUrl);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setConnectTimeout(getSinkTimeOutSeconds());
        connection.setReadTimeout(getSinkTimeOutSeconds());
        connection.setDoOutput(true);

        if (metrics != null) {
          String jsonData = mapper.writeValueAsString(metrics);
          try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonData.getBytes("UTF-8"));
          }
        }

        int statusCode = connection.getResponseCode();

        if (statusCode != 200) {
          LOG.info("Unable to POST metrics to external sink, " + connectUrl +
            ", statusCode = " + statusCode);
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Metrics posted to external sink " + connectUrl);
          }
        }
        cleanupInputStream(connection.getInputStream());

      } catch (IOException io) {
        LOG.warn("Unable to sink data to external system.", io);
      }
    }
  }
}
