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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.availability.MetricCollectorHAHelper;
import org.apache.hadoop.metrics2.sink.timeline.availability.MetricCollectorUnavailableException;
import org.apache.hadoop.metrics2.sink.timeline.availability.MetricSinkWriteShardHostnameHashingStrategy;
import org.apache.hadoop.metrics2.sink.timeline.availability.MetricSinkWriteShardStrategy;
import org.apache.http.HttpStatus;
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
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractTimelineMetricsSink {
  public static final String TAGS_FOR_PREFIX_PROPERTY_PREFIX = "tagsForPrefix.";
  public static final String MAX_METRIC_ROW_CACHE_SIZE = "maxRowCacheSize";
  public static final String METRICS_SEND_INTERVAL = "sendInterval";
  public static final String METRICS_POST_TIMEOUT_SECONDS = "timeout";
  public static final String COLLECTOR_HOSTS_PROPERTY = "collector.hosts";
  public static final String COLLECTOR_PROTOCOL = "protocol";
  public static final String COLLECTOR_PORT = "port";
  public static final String ZOOKEEPER_QUORUM = "zookeeper.quorum";
  public static final String COLLECTOR_ZOOKEEPER_QUORUM = "metrics.zookeeper.quorum";
  public static final int DEFAULT_POST_TIMEOUT_SECONDS = 10;
  public static final String SKIP_COUNTER_TRANSFROMATION = "skipCounterDerivative";
  public static final String RPC_METRIC_PREFIX = "metric.rpc";
  public static final String WS_V1_TIMELINE_METRICS = "/ws/v1/timeline/metrics";
  public static final String SSL_KEYSTORE_PATH_PROPERTY = "truststore.path";
  public static final String SSL_KEYSTORE_TYPE_PROPERTY = "truststore.type";
  public static final String SSL_KEYSTORE_PASSWORD_PROPERTY = "truststore.password";
  public static final String HOST_IN_MEMORY_AGGREGATION_ENABLED_PROPERTY = "host_in_memory_aggregation";
  public static final String HOST_IN_MEMORY_AGGREGATION_PORT_PROPERTY = "host_in_memory_aggregation_port";
  public static final String HOST_IN_MEMORY_AGGREGATION_PROTOCOL_PROPERTY = "host_in_memory_aggregation_protocol";
  public static final String COLLECTOR_LIVE_NODES_PATH = "/ws/v1/timeline/metrics/livenodes";
  public static final String INSTANCE_ID_PROPERTY = "instanceId";
  public static final String SET_INSTANCE_ID_PROPERTY = "set.instanceId";
  public static final String COOKIE = "Cookie";
  private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
  private static final String NEGOTIATE = "Negotiate";

  protected final AtomicInteger failedCollectorConnectionsCounter = new AtomicInteger(0);
  public static int NUMBER_OF_SKIPPED_COLLECTOR_EXCEPTIONS = 100;
  protected static final AtomicInteger nullCollectorCounter = new AtomicInteger(0);
  public static int NUMBER_OF_NULL_COLLECTOR_EXCEPTIONS = 20;
  public int ZK_CONNECT_TRY_COUNT = 10;
  public int ZK_SLEEP_BETWEEN_RETRY_TIME = 2000;
  public boolean shardExpired = true;
  private int zookeeperMinBackoffTimeMins = 2;
  private int zookeeperMaxBackoffTimeMins = 5;
  private long zookeeperBackoffTimeMillis;
  private long lastFailedZkRequestTime = 0l;

  private SSLSocketFactory sslSocketFactory;
  private AppCookieManager appCookieManager = null;

  protected final Log LOG;

  protected static ObjectMapper mapper;

  protected MetricCollectorHAHelper collectorHAHelper;

  protected MetricSinkWriteShardStrategy metricSinkWriteShardStrategy;

  // Single element cache with fixed expiration - Helps adjacent Sinks as
  // well as timed refresh
  protected Supplier<String> targetCollectorHostSupplier;

  protected final SortedSet<String> allKnownLiveCollectors = new TreeSet<>();

  private volatile boolean isInitializedForHA = false;

  @SuppressWarnings("all")
  private final int RETRY_COUNT_BEFORE_COLLECTOR_FAILOVER = 3;

  private final Gson gson = new Gson();

  private final Random rand = new Random();

  private static final int COLLECTOR_HOST_CACHE_MAX_EXPIRATION_MINUTES = 75;
  private static final int COLLECTOR_HOST_CACHE_MIN_EXPIRATION_MINUTES = 60;

  //10 seconds
  protected int collectionPeriodMillis = 10000;

  private int cacheExpireTimeMinutesDefault = 10;

  private volatile Cache<String, TimelineMetric> metricsPostCache = CacheBuilder.newBuilder().expireAfterAccess(cacheExpireTimeMinutesDefault, TimeUnit.MINUTES).build();

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

  /**
   * Initialize Sink write strategy with respect to HA Collector
   */
  protected void init() {
    metricSinkWriteShardStrategy = new MetricSinkWriteShardHostnameHashingStrategy(getHostname());
    collectorHAHelper = new MetricCollectorHAHelper(getZookeeperQuorum(),
      ZK_CONNECT_TRY_COUNT, ZK_SLEEP_BETWEEN_RETRY_TIME);
    zookeeperBackoffTimeMillis = getZookeeperBackoffTimeMillis();
    isInitializedForHA = true;
  }

  protected boolean emitMetricsJson(String connectUrl, String jsonData) {
    int timeout = getTimeoutSeconds() * 1000;
    HttpURLConnection connection = null;
    try {
      if (connectUrl == null) {
        throw new IOException("Unknown URL. Unable to connect to metrics collector.");
      }
      connection = connectUrl.startsWith("https") ?
          getSSLConnection(connectUrl) : getConnection(connectUrl);

      if (LOG.isDebugEnabled()) {
        LOG.debug("emitMetricsJson to " + connectUrl + ", " + jsonData);
      }
      AppCookieManager appCookieManager = getAppCookieManager();
      String appCookie = appCookieManager.getCachedAppCookie(connectUrl);
      if (appCookie != null) {
        if (LOG.isInfoEnabled()) {
          LOG.info("Using cached app cookie for URL:" + connectUrl);
        }
        connection.setRequestProperty(COOKIE, appCookie);
      }

      int statusCode = emitMetricsJson(connection, timeout, jsonData);

      if (statusCode == HttpStatus.SC_UNAUTHORIZED ) {
        String wwwAuthHeader = connection.getHeaderField(WWW_AUTHENTICATE);
        if (LOG.isInfoEnabled()) {
          LOG.info("Received WWW-Authentication header:" + wwwAuthHeader + ", for URL:" + connectUrl);
        }
        if (wwwAuthHeader != null && wwwAuthHeader.trim().startsWith(NEGOTIATE)) {
          appCookie = appCookieManager.getAppCookie(connectUrl, true);
          if (appCookie != null) {
            cleanupInputStream(connection.getInputStream());
            connection = connectUrl.startsWith("https") ?
                getSSLConnection(connectUrl) : getConnection(connectUrl);
            connection.setRequestProperty(COOKIE, appCookie);
            statusCode = emitMetricsJson(connection, timeout, jsonData);
          }
        } else {
          // no supported authentication type found
          // we would let the original response propagate
          LOG.error("Unsupported WWW-Authentication header:" + wwwAuthHeader+ ", for URL:" + connectUrl);
        }
      }

      if (statusCode != 200) {
        LOG.info("Unable to POST metrics to collector, " + connectUrl + ", " +
            "statusCode = " + statusCode);
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Metrics posted to Collector " + connectUrl);
        }
      }
      cleanupInputStream(connection.getInputStream());
      // reset failedCollectorConnectionsCounter to "0"
      failedCollectorConnectionsCounter.set(0);
      return true;
    } catch (IOException ioe) {
      StringBuilder errorMessage =
          new StringBuilder("Unable to connect to collector, " + connectUrl + "\n"
                  + "This exceptions will be ignored for next " + NUMBER_OF_SKIPPED_COLLECTOR_EXCEPTIONS + " times\n");
      try {
        if ((connection != null)) {
          errorMessage.append(cleanupInputStream(connection.getErrorStream()));
        }
      } catch (IOException e) {
        //NOP
      }

      if (failedCollectorConnectionsCounter.getAndIncrement() == 0) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(errorMessage, ioe);
        } else {
          LOG.info(errorMessage);
        }
        throw new UnableToConnectException(ioe).setConnectUrl(connectUrl);
      } else {
        failedCollectorConnectionsCounter.compareAndSet(NUMBER_OF_SKIPPED_COLLECTOR_EXCEPTIONS, 0);
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("Ignoring %s AMS connection exceptions", NUMBER_OF_SKIPPED_COLLECTOR_EXCEPTIONS));
        }
        return false;
      }
    }
  }

  private int emitMetricsJson(HttpURLConnection connection, int timeout, String jsonData) throws IOException {
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
    if (LOG.isDebugEnabled()) {
      LOG.debug("emitMetricsJson: statusCode = " + statusCode);
    }
    return statusCode;
  }

  protected String getCurrentCollectorHost() {
    String collectorHost;
    // Get cached target
    if (targetCollectorHostSupplier != null) {
      collectorHost = targetCollectorHostSupplier.get();
      // Last X attempts have failed - force refresh
      if (failedCollectorConnectionsCounter.get() > RETRY_COUNT_BEFORE_COLLECTOR_FAILOVER) {
        LOG.debug("Removing collector " + collectorHost + " from allKnownLiveCollectors.");
        allKnownLiveCollectors.remove(collectorHost);
        targetCollectorHostSupplier = null;
        collectorHost = findPreferredCollectHost();
      }
    } else {
      collectorHost = findPreferredCollectHost();
    }

    if (collectorHost == null) {
      if (nullCollectorCounter.getAndIncrement() == 0) {
        LOG.info("No live collector to send metrics to. Metrics to be sent will be discarded. " +
          "This message will be skipped for the next " + NUMBER_OF_NULL_COLLECTOR_EXCEPTIONS + " times.");
      } else {
        nullCollectorCounter.compareAndSet(NUMBER_OF_NULL_COLLECTOR_EXCEPTIONS, 0);
      }
    } else {
      nullCollectorCounter.set(0);
    }
    return collectorHost;
  }

  /**
   * @param metrics metrics to post, metric values will be aligned by minute mark,
   *                last uncompleted minute will be cached to post in future iteration
   */
  protected boolean emitMetrics(TimelineMetrics metrics) {
    return emitMetrics(metrics, false);
  }

  /**
   * @param metrics metrics to post, if postAllCachedMetrics is false metric values will be aligned by minute mark,
   *                last uncompleted minute will be cached to post in future iteration
   * @param postAllCachedMetrics if set to true all cached metrics will be posted, ignoring the minute aligning
   * @return
   */
  protected boolean emitMetrics(TimelineMetrics metrics, boolean postAllCachedMetrics) {
    String connectUrl;
    boolean validCollectorHost = true;

    if (isHostInMemoryAggregationEnabled()) {
      String hostname = "localhost";
      if (getHostInMemoryAggregationProtocol().equalsIgnoreCase("https")) {
        hostname = getHostname();
      }
      connectUrl = constructTimelineMetricUri(getHostInMemoryAggregationProtocol(), hostname, String.valueOf(getHostInMemoryAggregationPort()));
    } else {
      String collectorHost  = getCurrentCollectorHost();
      if (collectorHost == null) {
        validCollectorHost = false;
      }
      connectUrl = getCollectorUri(collectorHost);
    }

    TimelineMetrics metricsToEmit = alignMetricsByMinuteMark(metrics);

    if (postAllCachedMetrics) {
      for (TimelineMetric timelineMetric : metricsPostCache.asMap().values()) {
        metricsToEmit.addOrMergeTimelineMetric(timelineMetric);
      }
      metricsPostCache.invalidateAll();
    }

    if (validCollectorHost) {
      String jsonData = null;
      LOG.debug("EmitMetrics connectUrl = "  + connectUrl);
      try {
        jsonData = mapper.writeValueAsString(metricsToEmit);
      } catch (IOException e) {
        LOG.error("Unable to parse metrics", e);
      }
      if (jsonData != null) {
        return emitMetricsJson(connectUrl, jsonData);
      }
    }
    return false;
  }

  /**
   * Get the associated app cookie manager.
   *
   * @return the app cookie manager
   */
  public synchronized AppCookieManager getAppCookieManager() {
    if (appCookieManager == null) {
      appCookieManager = new AppCookieManager();
    }
    return appCookieManager;
  }

  /**
   * Align metrics by the minutes so that only complete minutes are send.
   * Not completed minutes data points will be cached and posted when the minute will be completed.
   * Cached metrics are merged with currently posting metrics
   * e.g:
   * first iteration if metrics from 00m15s to 01m15s are processed,
   *               then metrics from 00m15s to 00m59s will be posted
   *                        and from 01m00s to 01m15s will be cached
   * second iteration   metrics from 01m25s to 02m55s are processed,
   *     cached metrics from previous call will be merged with current,
   *                    metrics from 01m00s to 02m55s will be posted, cache will be empty
   * @param metrics
   * @return
   */
  protected TimelineMetrics alignMetricsByMinuteMark(TimelineMetrics metrics) {
    TimelineMetrics allMetricsToPost = new TimelineMetrics();

    for (TimelineMetric metric : metrics.getMetrics()) {
      TimelineMetric cachedMetric = metricsPostCache.getIfPresent(metric.getMetricName());
      if (cachedMetric != null) {
        metric.addMetricValues(cachedMetric.getMetricValues());
        metricsPostCache.invalidate(metric.getMetricName());
      }
    }

    for (TimelineMetric metric : metrics.getMetrics()) {
      TreeMap<Long, Double> valuesToCache = new TreeMap<>();
      TreeMap<Long, Double> valuesToPost = metric.getMetricValues();

      // in case there can't be any more datapoints in last minute just post the metrics,
      // otherwise need to cut off and cache the last uncompleted minute
      if (!(valuesToPost.lastKey() % 60000 > 60000 - collectionPeriodMillis)) {
        Long lastMinute = valuesToPost.lastKey() / 60000;
        while (!valuesToPost.isEmpty() && valuesToPost.lastKey() / 60000 == lastMinute) {
          valuesToCache.put(valuesToPost.lastKey(), valuesToPost.get(valuesToPost.lastKey()));
          valuesToPost.remove(valuesToPost.lastKey());
        }
      }

      if (!valuesToCache.isEmpty()) {
        TimelineMetric metricToCache = new TimelineMetric(metric);
        metricToCache.setMetricValues(valuesToCache);
        metricsPostCache.put(metricToCache.getMetricName(), metricToCache);
      }

      if (!valuesToPost.isEmpty()) {
        TimelineMetric metricToPost = new TimelineMetric(metric);
        metricToPost.setMetricValues(valuesToPost);
        allMetricsToPost.addOrMergeTimelineMetric(metricToPost);
      }
    }

    return allMetricsToPost;
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

  // Get a connection
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

  /**
   * Find appropriate write shard for this sink using the {@link org.apache.hadoop.metrics2.sink.timeline.availability.MetricSinkWriteShardStrategy}
   *
   * 1. Use configured collector(s) to discover available collectors
   * 2. If configured collector(s) are unresponsive check Zookeeper to find live hosts
   * 3. Refresh known collector list using ZK
   * 4. Default: Return configured collector with no side effect due to discovery.
   *
   * throws {#link MetricsSinkInitializationException} if called before
   * initialization, not other side effect
   *
   * @return String Collector hostname
   */
  protected synchronized String findPreferredCollectHost() {
    if (!isInitializedForHA) {
      init();
    }

    shardExpired = false;
    // Auto expire and re-calculate after 1 hour
    if (targetCollectorHostSupplier != null) {
      String targetCollector = targetCollectorHostSupplier.get();
      if (targetCollector != null) {
        return targetCollector;
      }
    }

    // Reach out to all configured collectors before Zookeeper
    Collection<String> collectorHosts = getConfiguredCollectorHosts();
    refreshCollectorsFromConfigured(collectorHosts);

    // Lookup Zookeeper for live hosts - max 10 seconds wait time
    long currentTime = System.currentTimeMillis();
    if (allKnownLiveCollectors.size() == 0 && getZookeeperQuorum() != null
      && (currentTime - lastFailedZkRequestTime) > zookeeperBackoffTimeMillis) {

      LOG.debug("No live collectors from configuration. Requesting zookeeper...");
      allKnownLiveCollectors.addAll(collectorHAHelper.findLiveCollectorHostsFromZNode());
      boolean noNewCollectorFromZk = true;
      for (String collectorHostFromZk : allKnownLiveCollectors) {
        if (!collectorHosts.contains(collectorHostFromZk)) {
          noNewCollectorFromZk = false;
          break;
        }
      }
      if (noNewCollectorFromZk) {
        LOG.debug("No new collector was found from Zookeeper. Will not request zookeeper for " + zookeeperBackoffTimeMillis + " millis");
        lastFailedZkRequestTime = System.currentTimeMillis();
      }
    }

    if (allKnownLiveCollectors.size() != 0) {
      targetCollectorHostSupplier = Suppliers.memoizeWithExpiration(
        new Supplier<String>() {
          @Override
          public String get() {
            //shardExpired flag is used to determine if the Supplier.get() is invoked through the
            // findPreferredCollectHost method (No need to refresh collector hosts
            // OR
            // through Expiry (Refresh needed to pick up dead collectors that might have not become alive).
            if (shardExpired) {
              refreshCollectorsFromConfigured(getConfiguredCollectorHosts());
            }
            return metricSinkWriteShardStrategy.findCollectorShard(new ArrayList<>(allKnownLiveCollectors));
          }
        },  // random.nextInt(max - min + 1) + min # (60 to 75 minutes)
        rand.nextInt(COLLECTOR_HOST_CACHE_MAX_EXPIRATION_MINUTES
          - COLLECTOR_HOST_CACHE_MIN_EXPIRATION_MINUTES + 1)
          + COLLECTOR_HOST_CACHE_MIN_EXPIRATION_MINUTES,
        TimeUnit.MINUTES
      );

      String collectorHost = targetCollectorHostSupplier.get();
      shardExpired = true;
      return collectorHost;
    }
    LOG.debug("Couldn't find any live collectors. Returning null");
    shardExpired = true;
    return null;
  }

  private void refreshCollectorsFromConfigured(Collection<String> collectorHosts) {

    LOG.debug("Trying to find live collector host from : " + collectorHosts);
    if (collectorHosts != null && !collectorHosts.isEmpty()) {
      for (String hostStr : collectorHosts) {
        hostStr = hostStr.trim();
        if (!hostStr.isEmpty()) {
          try {
            Collection<String> liveHosts = findLiveCollectorHostsFromKnownCollector(hostStr, getCollectorPort());
            // Update live Hosts - current host will already be a part of this
            for (String host : liveHosts) {
              allKnownLiveCollectors.add(host);
            }
            break; // Found at least 1 live collector
          } catch (MetricCollectorUnavailableException e) {
            LOG.debug("Collector " + hostStr + " is not longer live. Removing " +
              "it from list of know live collector hosts : " + allKnownLiveCollectors);
            allKnownLiveCollectors.remove(hostStr);
          }
        }
      }
    }
  }

  Collection<String> findLiveCollectorHostsFromKnownCollector(String host, String port) throws MetricCollectorUnavailableException {
    List<String> collectors = new ArrayList<>();
    HttpURLConnection connection = null;
    StringBuilder sb = new StringBuilder(getCollectorProtocol());
    sb.append("://");
    sb.append(host);
    sb.append(":");
    sb.append(port);
    sb.append(COLLECTOR_LIVE_NODES_PATH);
    String connectUrl = sb.toString();
    LOG.debug("Requesting live collector nodes : " + connectUrl);
    try {
      connection = getCollectorProtocol().startsWith("https") ?
        getSSLConnection(connectUrl) : getConnection(connectUrl);

      connection.setRequestMethod("GET");
      // 5 seconds for this op is plenty of wait time
      connection.setConnectTimeout(3000);
      connection.setReadTimeout(2000);

      int responseCode = connection.getResponseCode();
      if (responseCode == 200) {
        try (InputStream in = connection.getInputStream()) {
          StringWriter writer = new StringWriter();
          IOUtils.copy(in, writer);
          try {
            collectors = gson.fromJson(writer.toString(), new TypeToken<List<String>>(){}.getType());
          } catch (JsonSyntaxException jse) {
            // Swallow this at the behest of still trying to POST
            LOG.debug("Exception deserializing the json data on live " +
              "collector nodes.", jse);
          }
        }
      }

    } catch (IOException ioe) {
      StringBuilder errorMessage =
        new StringBuilder("Unable to connect to collector, " + connectUrl);
      try {
        if ((connection != null)) {
          errorMessage.append(cleanupInputStream(connection.getErrorStream()));
        }
      } catch (IOException e) {
        //NOP
      }
      LOG.debug(errorMessage);
      LOG.debug(ioe);
      String warnMsg = "Unable to connect to collector to find live nodes.";
      throw new MetricCollectorUnavailableException(warnMsg);
    }
    return collectors;
  }

  // Constructing without UriBuilder to avoid unfavorable httpclient
  // dependencies
  protected String constructTimelineMetricUri(String protocol, String host, String port) {
    StringBuilder sb = new StringBuilder(protocol);
    sb.append("://");
    sb.append(host);
    sb.append(":");
    sb.append(port);
    sb.append(WS_V1_TIMELINE_METRICS);
    return sb.toString();
  }

  /**
   * Parses input Sting of format "host1,host2" into Collection of hostnames
   */
  public Collection<String> parseHostsStringIntoCollection(String hostsString) {
    Set<String> hosts = new HashSet<>();

    if (StringUtils.isEmpty(hostsString)) {
      LOG.error("No Metric collector configured.");
      return hosts;
    }


    for (String host : hostsString.split(",")) {
      if (StringUtils.isEmpty(host))
        continue;
      hosts.add(host.trim());
    }

    return hosts;
  }


  private long getZookeeperBackoffTimeMillis() {
    return (zookeeperMinBackoffTimeMins +
      rand.nextInt(zookeeperMaxBackoffTimeMins - zookeeperMinBackoffTimeMins + 1)) * 60*1000l;
  }

  //for now it's used only for testing
  protected Cache<String, TimelineMetric> getMetricsPostCache() {
    return metricsPostCache;
  }

  /**
   * Get a pre-formatted URI for the collector
   */
  abstract protected String getCollectorUri(String host);

  abstract protected String getCollectorProtocol();

  abstract protected String getCollectorPort();

  /**
   * How soon to timeout on the emit calls in seconds.
   */
  abstract protected int getTimeoutSeconds();

  /**
   * Get the zookeeper quorum for the cluster used to find collector
   * @return String "host1:port1,host2:port2"
   */
  abstract protected String getZookeeperQuorum();

  /**
   * Get pre-configured list of collectors hosts available
   * @return Collection<String> host1,host2
   */
  abstract protected Collection<String> getConfiguredCollectorHosts();

  /**
   * Get hostname used for calculating write shard.
   * @return String "host1"
   */
  abstract protected String getHostname();

  /**
   * Check if host in-memory aggregation is enabled
   * @return
   */
  abstract protected boolean isHostInMemoryAggregationEnabled();

  /**
   * In memory aggregation port
   * @return
   */
  abstract protected int getHostInMemoryAggregationPort();

  /**
   * In memory aggregation protocol
   * @return
   */
  abstract protected String getHostInMemoryAggregationProtocol();
}
