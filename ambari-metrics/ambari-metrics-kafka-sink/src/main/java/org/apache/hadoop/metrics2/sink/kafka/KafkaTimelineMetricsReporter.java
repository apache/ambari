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

package org.apache.hadoop.metrics2.sink.kafka;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricProcessor;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Summarizable;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.stats.Snapshot;
import kafka.metrics.KafkaMetricsConfig;
import kafka.metrics.KafkaMetricsReporter;
import kafka.utils.VerifiableProperties;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata.MetricType;
import static org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache.MAX_EVICTION_TIME_MILLIS;
import static org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache.MAX_RECS_PER_NAME_DEFAULT;

public class KafkaTimelineMetricsReporter extends AbstractTimelineMetricsSink
    implements KafkaMetricsReporter, KafkaTimelineMetricsReporterMBean {

  private final static Log LOG = LogFactory.getLog(KafkaTimelineMetricsReporter.class);

  private static final String TIMELINE_METRICS_KAFKA_PREFIX = "kafka.timeline.metrics.";
  private static final String TIMELINE_METRICS_SEND_INTERVAL_PROPERTY = "sendInterval";
  private static final String TIMELINE_METRICS_MAX_ROW_CACHE_SIZE_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + "maxRowCacheSize";
  private static final String TIMELINE_HOSTS_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + "hosts";
  private static final String TIMELINE_PORT_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + "port";
  private static final String TIMELINE_PROTOCOL_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + "protocol";
  private static final String TIMELINE_REPORTER_ENABLED_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + "reporter.enabled";
  private static final String TIMELINE_METRICS_SSL_KEYSTORE_PATH_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + SSL_KEYSTORE_PATH_PROPERTY;
  private static final String TIMELINE_METRICS_SSL_KEYSTORE_TYPE_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + SSL_KEYSTORE_TYPE_PROPERTY;
  private static final String TIMELINE_METRICS_SSL_KEYSTORE_PASSWORD_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + SSL_KEYSTORE_PASSWORD_PROPERTY;
  private static final String TIMELINE_METRICS_KAFKA_INSTANCE_ID_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + INSTANCE_ID_PROPERTY;
  private static final String TIMELINE_METRICS_KAFKA_SET_INSTANCE_ID_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + SET_INSTANCE_ID_PROPERTY;
  private static final String TIMELINE_METRICS_KAFKA_HOST_IN_MEMORY_AGGREGATION_ENABLED_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + HOST_IN_MEMORY_AGGREGATION_ENABLED_PROPERTY;
  private static final String TIMELINE_METRICS_KAFKA_HOST_IN_MEMORY_AGGREGATION_PORT_PROPERTY = TIMELINE_METRICS_KAFKA_PREFIX + HOST_IN_MEMORY_AGGREGATION_PORT_PROPERTY;
  private static final String TIMELINE_DEFAULT_HOST = "localhost";
  private static final String TIMELINE_DEFAULT_PORT = "6188";
  private static final String TIMELINE_DEFAULT_PROTOCOL = "http";
  private static final String EXCLUDED_METRICS_PROPERTY = "external.kafka.metrics.exclude.prefix";
  private static final String INCLUDED_METRICS_PROPERTY = "external.kafka.metrics.include.prefix";

  private volatile boolean initialized = false;
  private boolean running = false;
  private final Object lock = new Object();
  private String hostname;
  private String metricCollectorPort;
  private Collection<String> collectorHosts;
  private String metricCollectorProtocol;
  private TimelineScheduledReporter reporter;
  private TimelineMetricsCache metricsCache;
  private int timeoutSeconds = 10;
  private String zookeeperQuorum = null;
  private boolean setInstanceId;
  private String instanceId;

  private String[] excludedMetricsPrefixes;
  private String[] includedMetricsPrefixes;
  // Local cache to avoid prefix matching everytime
  private Set<String> excludedMetrics = new HashSet<>();
  private boolean hostInMemoryAggregationEnabled;
  private int hostInMemoryAggregationPort;

  @Override
  protected String getCollectorUri(String host) {
    return constructTimelineMetricUri(metricCollectorProtocol, host, metricCollectorPort);
  }

  @Override
  protected String getCollectorProtocol() {
    return metricCollectorProtocol;
  }

  @Override
  protected String getCollectorPort() {
    return metricCollectorPort;
  }

  @Override
  protected int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  @Override
  protected String getZookeeperQuorum() {
    return zookeeperQuorum;
  }

  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return collectorHosts;
  }

  @Override
  protected String getHostname() {
    return hostname;
  }


  @Override
  protected boolean isHostInMemoryAggregationEnabled() {
    return hostInMemoryAggregationEnabled;
  }

  @Override
  protected int getHostInMemoryAggregationPort() {
    return hostInMemoryAggregationPort;
  }

  public void setMetricsCache(TimelineMetricsCache metricsCache) {
    this.metricsCache = metricsCache;
  }

  @Override
  public void init(VerifiableProperties props) {
    synchronized (lock) {
      if (!initialized) {
        LOG.info("Initializing Kafka Timeline Metrics Sink");
        try {
          hostname = InetAddress.getLocalHost().getHostName();
          //If not FQDN , call  DNS
          if ((hostname == null) || (!hostname.contains("."))) {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
          }
        } catch (UnknownHostException e) {
          LOG.error("Could not identify hostname.");
          throw new RuntimeException("Could not identify hostname.", e);
        }
        // Initialize the collector write strategy
        super.init();

        KafkaMetricsConfig metricsConfig = new KafkaMetricsConfig(props);
        timeoutSeconds = props.getInt(METRICS_POST_TIMEOUT_SECONDS, DEFAULT_POST_TIMEOUT_SECONDS);
        int metricsSendInterval = props.getInt(TIMELINE_METRICS_SEND_INTERVAL_PROPERTY, MAX_EVICTION_TIME_MILLIS);
        int maxRowCacheSize = props.getInt(TIMELINE_METRICS_MAX_ROW_CACHE_SIZE_PROPERTY, MAX_RECS_PER_NAME_DEFAULT);

        zookeeperQuorum = props.containsKey(COLLECTOR_ZOOKEEPER_QUORUM) ?
          props.getString(COLLECTOR_ZOOKEEPER_QUORUM) : props.getString("zookeeper.connect");

        metricCollectorPort = props.getString(TIMELINE_PORT_PROPERTY, TIMELINE_DEFAULT_PORT);
        collectorHosts = parseHostsStringIntoCollection(props.getString(TIMELINE_HOSTS_PROPERTY, TIMELINE_DEFAULT_HOST));
        metricCollectorProtocol = props.getString(TIMELINE_PROTOCOL_PROPERTY, TIMELINE_DEFAULT_PROTOCOL);

        instanceId = props.getString(TIMELINE_METRICS_KAFKA_INSTANCE_ID_PROPERTY, null);
        setInstanceId = props.getBoolean(TIMELINE_METRICS_KAFKA_SET_INSTANCE_ID_PROPERTY, false);

        hostInMemoryAggregationEnabled = props.getBoolean(TIMELINE_METRICS_KAFKA_HOST_IN_MEMORY_AGGREGATION_ENABLED_PROPERTY, false);
        hostInMemoryAggregationPort = props.getInt(TIMELINE_METRICS_KAFKA_HOST_IN_MEMORY_AGGREGATION_PORT_PROPERTY, 61888);
        setMetricsCache(new TimelineMetricsCache(maxRowCacheSize, metricsSendInterval));

        if (metricCollectorProtocol.contains("https")) {
          String trustStorePath = props.getString(TIMELINE_METRICS_SSL_KEYSTORE_PATH_PROPERTY).trim();
          String trustStoreType = props.getString(TIMELINE_METRICS_SSL_KEYSTORE_TYPE_PROPERTY).trim();
          String trustStorePwd = props.getString(TIMELINE_METRICS_SSL_KEYSTORE_PASSWORD_PROPERTY).trim();
          loadTruststore(trustStorePath, trustStoreType, trustStorePwd);
        }

        // Exclusion policy
        String excludedMetricsStr = props.getString(EXCLUDED_METRICS_PROPERTY, "");
        if (!StringUtils.isEmpty(excludedMetricsStr.trim())) {
          excludedMetricsPrefixes = excludedMetricsStr.trim().split(",");
        }
        // Inclusion override
        String includedMetricsStr = props.getString(INCLUDED_METRICS_PROPERTY, "");
        if (!StringUtils.isEmpty(includedMetricsStr.trim())) {
          includedMetricsPrefixes = includedMetricsStr.trim().split(",");
        }

        initializeReporter();
        if (props.getBoolean(TIMELINE_REPORTER_ENABLED_PROPERTY, false)) {
          startReporter(metricsConfig.pollingIntervalSecs());
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("MetricsSendInterval = " + metricsSendInterval);
          LOG.debug("MaxRowCacheSize = " + maxRowCacheSize);
          LOG.debug("Excluded metrics prefixes = " + excludedMetricsStr);
          LOG.debug("Included metrics prefixes = " + includedMetricsStr);
        }
      }
    }
  }

  public String getMBeanName() {
    return "kafka:type=org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter";
  }

  public synchronized void startReporter(long period) {
    synchronized (lock) {
      if (initialized && !running) {
        reporter.start(period, TimeUnit.SECONDS);
        running = true;
        LOG.info(String.format("Started Kafka Timeline metrics reporter with polling period %d seconds", period));
      }
    }
  }

  public synchronized void stopReporter() {
    synchronized (lock) {
      if (initialized && running) {
        reporter.stop();
        running = false;
        LOG.info("Stopped Kafka Timeline metrics reporter");
        initializeReporter();
      }
    }
  }

  private void initializeReporter() {
    reporter = new TimelineScheduledReporter(Metrics.defaultRegistry(), "timeline-scheduled-reporter",
        TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
    initialized = true;
  }

  interface Context {
    public List<TimelineMetric> getTimelineMetricList();
  }

  protected boolean isExcludedMetric(String metricName) {
    if (excludedMetrics.contains(metricName)) {
      return true;
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace("metricName => " + metricName +
        ", exclude: " + StringUtils.startsWithAny(metricName, excludedMetricsPrefixes) +
        ", include: " + StringUtils.startsWithAny(metricName, includedMetricsPrefixes));
    }
    if (StringUtils.startsWithAny(metricName, excludedMetricsPrefixes)) {
      if (!StringUtils.startsWithAny(metricName, includedMetricsPrefixes)) {
        excludedMetrics.add(metricName);
        return true;
      }
    }
    return false;
  }

  class TimelineScheduledReporter extends ScheduledReporter implements MetricProcessor<Context> {

    private static final String APP_ID = "kafka_broker";
    private static final String COUNT_SUFIX = ".count";
    private static final String ONE_MINUTE_RATE_SUFIX = ".1MinuteRate";
    private static final String MEAN_SUFIX = ".mean";
    private static final String MEAN_RATE_SUFIX = ".meanRate";
    private static final String FIVE_MINUTE_RATE_SUFIX = ".5MinuteRate";
    private static final String FIFTEEN_MINUTE_RATE_SUFIX = ".15MinuteRate";
    private static final String MIN_SUFIX = ".min";
    private static final String MAX_SUFIX = ".max";
    private static final String MEDIAN_SUFIX = ".median";
    private static final String STD_DEV_SUFIX = "stddev";
    private static final String SEVENTY_FIFTH_PERCENTILE_SUFIX = ".75percentile";
    private static final String NINETY_FIFTH_PERCENTILE_SUFIX = ".95percentile";
    private static final String NINETY_EIGHTH_PERCENTILE_SUFIX = ".98percentile";
    private static final String NINETY_NINTH_PERCENTILE_SUFIX = ".99percentile";
    private static final String NINETY_NINE_POINT_NINE_PERCENTILE_SUFIX = ".999percentile";

    protected TimelineScheduledReporter(MetricsRegistry registry, String name, TimeUnit rateUnit, TimeUnit durationUnit) {
      super(registry, name, rateUnit, durationUnit);
    }

    @Override
    public void report(Set<Entry<MetricName, Metric>> metrics) {
      final List<TimelineMetric> metricsList = new ArrayList<TimelineMetric>();
      try {
        for (Entry<MetricName, Metric> entry : metrics) {
          final MetricName metricName = entry.getKey();
          final Metric metric = entry.getValue();
          Context context = new Context() {
            public List<TimelineMetric> getTimelineMetricList() {
              return metricsList;
            }
          };
          metric.processWith(this, metricName, context);
        }
      } catch (Throwable t) {
        LOG.error("Exception processing Kafka metric", t);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Metrics List size: " + metricsList.size());
        LOG.debug("Metics Set size: " + metrics.size());
        LOG.debug("Excluded metrics set: " + excludedMetrics);
      }

      if (!metricsList.isEmpty()) {
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        timelineMetrics.setMetrics(metricsList);
        try {
          emitMetrics(timelineMetrics);
        } catch (Throwable t) {
          LOG.error("Exception emitting metrics", t);
        }
      }
    }

    private TimelineMetric createTimelineMetric(long currentTimeMillis, String component, String attributeName,
        Number attributeValue) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Creating timeline metric: " + attributeName + " = " + attributeValue + " time = "
            + currentTimeMillis + " app_id = " + component);
      }
      TimelineMetric timelineMetric = new TimelineMetric();
      timelineMetric.setMetricName(attributeName);
      timelineMetric.setHostName(hostname);
      if (setInstanceId) {
        timelineMetric.setInstanceId(instanceId);
      }
      timelineMetric.setAppId(component);
      timelineMetric.setStartTime(currentTimeMillis);
      timelineMetric.setType(ClassUtils.getShortCanonicalName(attributeValue, "Number"));
      timelineMetric.getMetricValues().put(currentTimeMillis, attributeValue.doubleValue());
      return timelineMetric;
    }

    @Override
    public void processMeter(MetricName name, Metered meter, Context context) throws Exception {
      final long currentTimeMillis = System.currentTimeMillis();
      final String sanitizedName = sanitizeName(name);

      String[] metricNames = cacheKafkaMetered(currentTimeMillis, sanitizedName, meter);

      populateMetricsList(context, MetricType.GAUGE, metricNames);
    }

    @Override
    public void processCounter(MetricName name, Counter counter, Context context) throws Exception {
      final long currentTimeMillis = System.currentTimeMillis();
      final String sanitizedName = sanitizeName(name);

      final String metricCountName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          COUNT_SUFIX, counter.count());

      populateMetricsList(context, MetricType.COUNTER, metricCountName);
    }

    @Override
    public void processHistogram(MetricName name, Histogram histogram, Context context) throws Exception {
      final long currentTimeMillis = System.currentTimeMillis();
      final Snapshot snapshot = histogram.getSnapshot();
      final String sanitizedName = sanitizeName(name);

      String[] metricHNames = cacheKafkaSummarizable(currentTimeMillis, sanitizedName, histogram);
      String[] metricSNames = cacheKafkaSnapshot(currentTimeMillis, sanitizedName, snapshot);

      String[] metricNames = (String[]) ArrayUtils.addAll(metricHNames, metricSNames);

      populateMetricsList(context, MetricType.GAUGE, metricNames);
    }

    @Override
    public void processTimer(MetricName name, Timer timer, Context context) throws Exception {
      final long currentTimeMillis = System.currentTimeMillis();
      final Snapshot snapshot = timer.getSnapshot();
      final String sanitizedName = sanitizeName(name);

      String[] metricMNames = cacheKafkaMetered(currentTimeMillis, sanitizedName, timer);
      String[] metricTNames = cacheKafkaSummarizable(currentTimeMillis, sanitizedName, timer);
      String[] metricSNames = cacheKafkaSnapshot(currentTimeMillis, sanitizedName, snapshot);

      String[] metricNames = (String[]) ArrayUtils.addAll(metricMNames, metricTNames);
      metricNames = (String[]) ArrayUtils.addAll(metricNames, metricSNames);

      populateMetricsList(context, MetricType.GAUGE, metricNames);
    }

    @Override
    public void processGauge(MetricName name, Gauge<?> gauge, Context context) throws Exception {
      final long currentTimeMillis = System.currentTimeMillis();
      final String sanitizedName = sanitizeName(name);

      try {
        if (!isExcludedMetric(sanitizedName)) {
          cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName, "", Double.parseDouble(String.valueOf(gauge.value())));
          populateMetricsList(context, MetricType.GAUGE, sanitizedName);
        }
      } catch (NumberFormatException ex) {
        LOG.debug(ex.getMessage());
      }
    }

    private String[] cacheKafkaMetered(long currentTimeMillis, String sanitizedName, Metered meter) {
      final String meterCountName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          COUNT_SUFIX, meter.count());
      final String meterOneMinuteRateName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          ONE_MINUTE_RATE_SUFIX, meter.oneMinuteRate());
      final String meterMeanRateName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          MEAN_RATE_SUFIX, meter.meanRate());
      final String meterFiveMinuteRateName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          FIVE_MINUTE_RATE_SUFIX, meter.fiveMinuteRate());
      final String meterFifteenMinuteRateName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          FIFTEEN_MINUTE_RATE_SUFIX, meter.fifteenMinuteRate());

      return new String[] { meterCountName, meterOneMinuteRateName, meterMeanRateName,
          meterFiveMinuteRateName, meterFifteenMinuteRateName };
    }

    private String[] cacheKafkaSummarizable(long currentTimeMillis, String sanitizedName, Summarizable summarizable) {
      final String minName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          MIN_SUFIX, summarizable.min());
      final String maxName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          MAX_SUFIX, summarizable.max());
      final String meanName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          MEAN_SUFIX, summarizable.mean());
      final String stdDevName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          STD_DEV_SUFIX, summarizable.stdDev());

      return new String[] { maxName, meanName, minName, stdDevName };
    }

    private String[] cacheKafkaSnapshot(long currentTimeMillis, String sanitizedName, Snapshot snapshot) {
      final String medianName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          MEDIAN_SUFIX, snapshot.getMedian());
      final String seventyFifthPercentileName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          SEVENTY_FIFTH_PERCENTILE_SUFIX, snapshot.get75thPercentile());
      final String ninetyFifthPercentileName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          NINETY_FIFTH_PERCENTILE_SUFIX, snapshot.get95thPercentile());
      final String ninetyEighthPercentileName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          NINETY_EIGHTH_PERCENTILE_SUFIX, snapshot.get98thPercentile());
      final String ninetyNinthPercentileName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
          NINETY_NINTH_PERCENTILE_SUFIX, snapshot.get99thPercentile());
      final String ninetyNinePointNinePercentileName = cacheSanitizedTimelineMetric(currentTimeMillis, sanitizedName,
        NINETY_NINE_POINT_NINE_PERCENTILE_SUFIX, snapshot.get999thPercentile());

      return new String[] { medianName,
          ninetyEighthPercentileName, ninetyFifthPercentileName, ninetyNinePointNinePercentileName,
          ninetyNinthPercentileName, seventyFifthPercentileName };
    }

    private String cacheSanitizedTimelineMetric(long currentTimeMillis, String sanitizedName, String suffix, Number metricValue) {
      final String meterName = sanitizedName + suffix;
      final TimelineMetric metric = createTimelineMetric(currentTimeMillis, APP_ID, meterName, metricValue);
      // Skip cache if we decide not to include the metric
      // Cannot do this before calculations of percentiles
      if (!isExcludedMetric(meterName)) {
        metricsCache.putTimelineMetric(metric);
      }
      return meterName;
    }

    private void populateMetricsList(Context context, MetricType type, String... metricNames) {
      for (String metricName : metricNames) {
        TimelineMetric cachedMetric = metricsCache.getTimelineMetric(metricName);
        if (cachedMetric != null) {
          cachedMetric.setType(type.name());
          context.getTimelineMetricList().add(cachedMetric);
        }
      }
    }

    protected String sanitizeName(MetricName name) {
      if (name == null) {
        return "";
      }
      final String qualifiedTypeName = name.getGroup() + "." + name.getType() + "." + name.getName();
      final String metricName = name.hasScope() ? qualifiedTypeName + '.' + name.getScope() : qualifiedTypeName;
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < metricName.length(); i++) {
        final char p = metricName.charAt(i);
        if (!(p >= 'A' && p <= 'Z') && !(p >= 'a' && p <= 'z') && !(p >= '0' && p <= '9') && (p != '_') && (p != '-')
            && (p != '.') && (p != '\0')) {
          sb.append('_');
        } else {
          sb.append(p);
        }
      }
      return sb.toString();
    }

  }
}
