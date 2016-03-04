/**
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
package org.apache.hadoop.metrics2.sink.timeline;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
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

public abstract class AbstractTimelineMetricsSink {
  public static final String TAGS_FOR_PREFIX_PROPERTY_PREFIX = "tagsForPrefix.";
  public static final String MAX_METRIC_ROW_CACHE_SIZE = "maxRowCacheSize";
  public static final String METRICS_SEND_INTERVAL = "sendInterval";
  public static final String METRICS_POST_TIMEOUT_SECONDS = "timeout";
  public static final String COLLECTOR_PROPERTY = "collector";
  public static final int DEFAULT_POST_TIMEOUT_SECONDS = 10;
  public static final String SKIP_COUNTER_TRANSFROMATION = "skipCounterDerivative";
  public static final String RPC_METRIC_PREFIX = "metric.rpc";
  public static final String RPC_METRIC_NAME_SUFFIX = "suffix";
  public static final String RPC_METRIC_PORT_SUFFIX = "port";

  public static final String WS_V1_TIMELINE_METRICS = "/ws/v1/timeline/metrics";

  public static final String SSL_KEYSTORE_PATH_PROPERTY = "truststore.path";
  public static final String SSL_KEYSTORE_TYPE_PROPERTY = "truststore.type";
  public static final String SSL_KEYSTORE_PASSWORD_PROPERTY = "truststore.password";

  private SSLSocketFactory sslSocketFactory;

  protected final Log LOG;

  protected static ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    mapper.getSerializationConfig()
      .withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
  }

  public AbstractTimelineMetricsSink() {
    LOG = LogFactory.getLog(this.getClass());
  }

  protected void emitMetrics(TimelineMetrics metrics) {
    String connectUrl = getCollectorUri();
    int timeout = getTimeoutSeconds() * 1000;
    HttpURLConnection connection = null;
    try {
      if (connectUrl == null) {
        throw new IOException("Unknown URL. Unable to connect to metrics collector.");
      }
      String jsonData = mapper.writeValueAsString(metrics);
      connection = connectUrl.startsWith("https") ?
        getSSLConnection(connectUrl) : getConnection(connectUrl);

      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Connection", "Keep-Alive");
      connection.setConnectTimeout(timeout);
      connection.setReadTimeout(timeout);
      connection.setDoOutput(true);

      if (jsonData != null) {
        try (OutputStream os = connection.getOutputStream()) {
          os.write(jsonData.getBytes("UTF-8"));
        }
      }

      int statusCode = connection.getResponseCode();

      if (statusCode != 200) {
        LOG.info("Unable to POST metrics to collector, " + connectUrl + ", " +
          "statusCode = " + statusCode);
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Metrics posted to Collector " + connectUrl);
        }
      }
      cleanupInputStream(connection.getInputStream());
    } catch (IOException ioe) {
      StringBuilder errorMessage =
        new StringBuilder("Unable to connect to collector, " + connectUrl + "\n");
      try {
        if ((connection != null)) {
          errorMessage.append(cleanupInputStream(connection.getErrorStream()));
        }
      } catch (IOException e) {
        //NOP
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug(errorMessage, ioe);
      } else {
        LOG.info(errorMessage);
      }
      throw new UnableToConnectException(ioe).setConnectUrl(connectUrl);
    }
  }

  /**
   * Cleans up and closes an input stream
   * see http://docs.oracle.com/javase/6/docs/technotes/guides/net/http-keepalive.html
   * @param is the InputStream to clean up
   * @return string read from the InputStream
   * @throws IOException
   */
  private String cleanupInputStream(InputStream is) throws IOException {
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

  // Get a connection
  protected HttpURLConnection getConnection(String spec) throws IOException {
    return (HttpURLConnection) new URL(spec).openConnection();
  }

  // Get an ssl connection
  protected HttpsURLConnection getSSLConnection(String spec)
    throws IOException, IllegalStateException {

    HttpsURLConnection connection = (HttpsURLConnection) (new URL(spec)
      .openConnection());

    connection.setSSLSocketFactory(sslSocketFactory);

    return connection;
  }

  protected void loadTruststore(String trustStorePath, String trustStoreType,
                                String trustStorePassword) {
    if (sslSocketFactory == null) {
      if (trustStorePath == null || trustStorePassword == null) {

        String msg =
          String.format("Can't load TrustStore. " +
            "Truststore path or password is not set.");

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

  abstract protected String getCollectorUri();

  abstract protected int getTimeoutSeconds();
}
