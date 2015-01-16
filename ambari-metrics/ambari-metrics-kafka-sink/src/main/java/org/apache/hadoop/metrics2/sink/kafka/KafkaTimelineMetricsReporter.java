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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import kafka.metrics.KafkaMetricsConfig;
import kafka.metrics.KafkaMetricsReporter;
import kafka.utils.VerifiableProperties;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.apache.hadoop.metrics2.util.Servers;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricProcessor;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.stats.Snapshot;

public class KafkaTimelineMetricsReporter extends AbstractTimelineMetricsSink implements KafkaMetricsReporter,
    KafkaTimelineMetricsReporterMBean {

  private final static Log LOG = LogFactory.getLog(KafkaTimelineMetricsReporter.class);

  private static final String TIMELINE_METRICS_SEND_INTERVAL_PROPERTY = "kafka.timeline.metrics.sendInterval";
  private static final String TIMELINE_METRICS_MAX_ROW_CACHE_SIZE_PROPERTY = "kafka.timeline.metrics.maxRowCacheSize";
  private static final String TIMELINE_HOST_PROPERTY = "kafka.timeline.metrics.host";
  private static final String TIMELINE_PORT_PROPERTY = "kafka.timeline.metrics.port";
  private static final String TIMELINE_REPORTER_ENABLED_PROPERTY = "kafka.timeline.metrics.reporter.enabled";
  private static final String TIMELINE_DEFAULT_HOST = "localhost";
  private static final String TIMELINE_DEFAULT_PORT = "8188";

  private boolean initialized = false;
  private boolean running = false;
  private Object lock = new Object();
  private String collectorUri;
  private String hostname;
  private SocketAddress socketAddress;
  private TimelineScheduledReporter reporter;
  private TimelineMetricsCache metricsCache;

  @Override
  protected SocketAddress getServerSocketAddress() {
    return socketAddress;
  }

  @Override
  protected String getCollectorUri() {
    return collectorUri;
  }

  public void setMetricsCache(TimelineMetricsCache metricsCache) {
    this.metricsCache = metricsCache;
  }

  public void init(VerifiableProperties props) {
    synchronized (lock) {
      if (!initialized) {
        LOG.info("Initializing Kafka Timeline Metrics Sink");
        try {
          hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
          LOG.error("Could not identify hostname.");
          throw new RuntimeException("Could not identify hostname.", e);
        }
        KafkaMetricsConfig metricsConfig = new KafkaMetricsConfig(props);
        int metricsSendInterval = Integer.parseInt(props.getString(TIMELINE_METRICS_SEND_INTERVAL_PROPERTY,
            String.valueOf(TimelineMetricsCache.MAX_EVICTION_TIME_MILLIS)));
        int maxRowCacheSize = Integer.parseInt(props.getString(TIMELINE_METRICS_MAX_ROW_CACHE_SIZE_PROPERTY,
            String.valueOf(TimelineMetricsCache.MAX_RECS_PER_NAME_DEFAULT)));
        String metricCollectorHost = props.getString(TIMELINE_HOST_PROPERTY, TIMELINE_DEFAULT_HOST);
        String metricCollectorPort = props.getString(TIMELINE_PORT_PROPERTY, TIMELINE_DEFAULT_PORT);
        setMetricsCache(new TimelineMetricsCache(maxRowCacheSize, metricsSendInterval));
        collectorUri = "http://" + metricCollectorHost + ":" + metricCollectorPort + "/ws/v1/timeline/metrics";
        List<InetSocketAddress> socketAddresses = Servers.parse(metricCollectorHost,
            Integer.parseInt(metricCollectorPort));
        if (socketAddresses != null && !socketAddresses.isEmpty()) {
          socketAddress = socketAddresses.get(0);
        }
        initializeReporter();
        if (props.getBoolean(TIMELINE_REPORTER_ENABLED_PROPERTY, false)) {
          startReporter(metricsConfig.pollingIntervalSecs());
        }
        if (LOG.isTraceEnabled()) {
          LOG.trace("CollectorUri = " + collectorUri);
          LOG.trace("SocketAddress = " + socketAddress);
          LOG.trace("MetricsSendInterval = " + metricsSendInterval);
          LOG.trace("MaxRowCacheSize = " + maxRowCacheSize);
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
      if (LOG.isTraceEnabled()) {
        LOG.trace("Metrics List size: " + metricsList.size());
        LOG.trace("Metics Set size: " + metrics.size());
      }
      if (!metricsList.isEmpty()) {
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        timelineMetrics.setMetrics(metricsList);
        try {
          emitMetrics(timelineMetrics);
        } catch (IOException e) {
          LOG.error("Unexpected error", e);
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
      final String meterCountName = sanitizedName + COUNT_SUFIX;
      final TimelineMetric countMetric = createTimelineMetric(currentTimeMillis, APP_ID, meterCountName, meter.count());

      final String meterOneMinuteRateName = sanitizedName + ONE_MINUTE_RATE_SUFIX;
      final TimelineMetric oneMinuteRateMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          meterOneMinuteRateName, meter.oneMinuteRate());

      final String meterMeanRateName = sanitizedName + MEAN_RATE_SUFIX;
      final TimelineMetric meanMetric = createTimelineMetric(currentTimeMillis, APP_ID, meterMeanRateName,
          meter.meanRate());

      final String meterFiveMinuteRateName = sanitizedName + FIVE_MINUTE_RATE_SUFIX;
      final TimelineMetric fiveMinuteRateMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          meterFiveMinuteRateName, meter.fiveMinuteRate());

      final String meterFifteenMinuteRateName = sanitizedName + FIFTEEN_MINUTE_RATE_SUFIX;
      final TimelineMetric fifteenMinuteRateMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          meterFifteenMinuteRateName, meter.fifteenMinuteRate());

      metricsCache.putTimelineMetric(countMetric);
      metricsCache.putTimelineMetric(oneMinuteRateMetric);
      metricsCache.putTimelineMetric(meanMetric);
      metricsCache.putTimelineMetric(fiveMinuteRateMetric);
      metricsCache.putTimelineMetric(fifteenMinuteRateMetric);

      String[] metricNames = new String[] { meterCountName, meterOneMinuteRateName, meterMeanRateName,
          meterFiveMinuteRateName, meterFifteenMinuteRateName };
      populateMetricsList(context, metricNames);
    }

    @Override
    public void processCounter(MetricName name, Counter counter, Context context) throws Exception {
      final long currentTimeMillis = System.currentTimeMillis();
      final String sanitizedName = sanitizeName(name);
      final String metricCountName = sanitizedName + COUNT_SUFIX;
      final TimelineMetric metric = createTimelineMetric(currentTimeMillis, APP_ID, metricCountName, counter.count());
      metricsCache.putTimelineMetric(metric);
      populateMetricsList(context, metricCountName);
    }

    @Override
    public void processHistogram(MetricName name, Histogram histogram, Context context) throws Exception {
      final long currentTimeMillis = System.currentTimeMillis();
      final Snapshot snapshot = histogram.getSnapshot();
      final String sanitizedName = sanitizeName(name);

      final String histogramMinName = sanitizedName + MIN_SUFIX;
      final TimelineMetric minMetric = createTimelineMetric(currentTimeMillis, APP_ID, histogramMinName,
          histogram.min());

      final String histogramMaxName = sanitizedName + MAX_SUFIX;
      final TimelineMetric maxMetric = createTimelineMetric(currentTimeMillis, APP_ID, histogramMaxName,
          histogram.max());

      final String histogramMeanName = sanitizedName + MEAN_SUFIX;
      final TimelineMetric meanMetric = createTimelineMetric(currentTimeMillis, APP_ID, histogramMeanName,
          histogram.mean());

      final String histogramMedianName = sanitizedName + MEDIAN_SUFIX;
      final TimelineMetric medianMetric = createTimelineMetric(currentTimeMillis, APP_ID, histogramMedianName,
          snapshot.getMedian());

      final String histogramStdDevName = sanitizedName + STD_DEV_SUFIX;
      final TimelineMetric stdDevMetric = createTimelineMetric(currentTimeMillis, APP_ID, histogramStdDevName,
          histogram.stdDev());

      final String histogramSeventyFifthPercentileName = sanitizedName + SEVENTY_FIFTH_PERCENTILE_SUFIX;
      final TimelineMetric seventyFifthPercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          histogramSeventyFifthPercentileName, snapshot.get75thPercentile());

      final String histogramNinetyFifthPercentileName = sanitizedName + NINETY_FIFTH_PERCENTILE_SUFIX;
      final TimelineMetric nintyFifthPercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          histogramNinetyFifthPercentileName, snapshot.get95thPercentile());

      final String histogramNinetyEighthPercentileName = sanitizedName + NINETY_EIGHTH_PERCENTILE_SUFIX;
      final TimelineMetric nintyEighthPercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          histogramNinetyEighthPercentileName, snapshot.get98thPercentile());

      final String histogramNinetyNinethPercentileName = sanitizedName + NINETY_NINTH_PERCENTILE_SUFIX;
      final TimelineMetric nintyNinthPercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          histogramNinetyNinethPercentileName, snapshot.get99thPercentile());

      final String histogramNinetyNinePointNinePercentileName = sanitizedName + NINETY_NINE_POINT_NINE_PERCENTILE_SUFIX;
      final TimelineMetric nintyNinthPointNinePercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          histogramNinetyNinePointNinePercentileName, snapshot.get999thPercentile());

      metricsCache.putTimelineMetric(minMetric);
      metricsCache.putTimelineMetric(maxMetric);
      metricsCache.putTimelineMetric(meanMetric);
      metricsCache.putTimelineMetric(medianMetric);
      metricsCache.putTimelineMetric(stdDevMetric);
      metricsCache.putTimelineMetric(seventyFifthPercentileMetric);
      metricsCache.putTimelineMetric(nintyFifthPercentileMetric);
      metricsCache.putTimelineMetric(nintyEighthPercentileMetric);
      metricsCache.putTimelineMetric(nintyNinthPercentileMetric);
      metricsCache.putTimelineMetric(nintyNinthPointNinePercentileMetric);

      String[] metricNames = new String[] { histogramMaxName, histogramMeanName, histogramMedianName, histogramMinName,
          histogramNinetyEighthPercentileName, histogramNinetyFifthPercentileName,
          histogramNinetyNinePointNinePercentileName, histogramNinetyNinethPercentileName,
          histogramSeventyFifthPercentileName, histogramStdDevName };
      populateMetricsList(context, metricNames);
    }

    @Override
    public void processTimer(MetricName name, Timer timer, Context context) throws Exception {
      final long currentTimeMillis = System.currentTimeMillis();
      final Snapshot snapshot = timer.getSnapshot();
      final String sanitizedName = sanitizeName(name);

      final String timerMinName = sanitizedName + MIN_SUFIX;
      final TimelineMetric minMetric = createTimelineMetric(currentTimeMillis, APP_ID, timerMinName, timer.min());

      final String timerMaxName = sanitizedName + MAX_SUFIX;
      final TimelineMetric maxMetric = createTimelineMetric(currentTimeMillis, APP_ID, timerMaxName, timer.max());

      final String timerMeanName = sanitizedName + MEAN_SUFIX;
      final TimelineMetric meanMetric = createTimelineMetric(currentTimeMillis, APP_ID, timerMeanName, timer.mean());

      final String timerMedianName = sanitizedName + MEDIAN_SUFIX;
      final TimelineMetric medianMetric = createTimelineMetric(currentTimeMillis, APP_ID, timerMedianName,
          snapshot.getMedian());

      final String timerStdDevName = sanitizedName + STD_DEV_SUFIX;
      final TimelineMetric stdDevMetric = createTimelineMetric(currentTimeMillis, APP_ID, timerStdDevName,
          timer.stdDev());

      final String timerSeventyFifthPercentileName = sanitizedName + SEVENTY_FIFTH_PERCENTILE_SUFIX;
      final TimelineMetric seventyFifthPercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          timerSeventyFifthPercentileName, snapshot.get75thPercentile());

      final String timerNinetyFifthPercentileName = sanitizedName + NINETY_FIFTH_PERCENTILE_SUFIX;
      final TimelineMetric nintyFifthPercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          timerNinetyFifthPercentileName, snapshot.get95thPercentile());

      final String timerNinetyEighthPercentileName = sanitizedName + NINETY_EIGHTH_PERCENTILE_SUFIX;
      final TimelineMetric nintyEighthPercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          timerNinetyEighthPercentileName, snapshot.get98thPercentile());

      final String timerNinetyNinthPercentileName = sanitizedName + NINETY_NINTH_PERCENTILE_SUFIX;
      final TimelineMetric nintyNinthPercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          timerNinetyNinthPercentileName, snapshot.get99thPercentile());

      final String timerNinetyNinePointNinePercentileName = sanitizedName + NINETY_NINE_POINT_NINE_PERCENTILE_SUFIX;
      final TimelineMetric nintyNinthPointNinePercentileMetric = createTimelineMetric(currentTimeMillis, APP_ID,
          timerNinetyNinePointNinePercentileName, snapshot.get999thPercentile());

      metricsCache.putTimelineMetric(minMetric);
      metricsCache.putTimelineMetric(maxMetric);
      metricsCache.putTimelineMetric(meanMetric);
      metricsCache.putTimelineMetric(medianMetric);
      metricsCache.putTimelineMetric(stdDevMetric);
      metricsCache.putTimelineMetric(seventyFifthPercentileMetric);
      metricsCache.putTimelineMetric(nintyFifthPercentileMetric);
      metricsCache.putTimelineMetric(nintyEighthPercentileMetric);
      metricsCache.putTimelineMetric(nintyNinthPercentileMetric);
      metricsCache.putTimelineMetric(nintyNinthPointNinePercentileMetric);

      String[] metricNames = new String[] { timerMaxName, timerMeanName, timerMedianName, timerMinName,
          timerNinetyEighthPercentileName, timerNinetyFifthPercentileName, timerNinetyNinePointNinePercentileName,
          timerNinetyNinthPercentileName, timerSeventyFifthPercentileName, timerStdDevName };
      populateMetricsList(context, metricNames);
    }

    @Override
    public void processGauge(MetricName name, Gauge<?> gauge, Context context) throws Exception {
      final long currentTimeMillis = System.currentTimeMillis();
      final String sanitizedName = sanitizeName(name);
      final TimelineMetric metric = createTimelineMetric(currentTimeMillis, APP_ID, sanitizedName,
          Double.parseDouble(String.valueOf(gauge.value())));
      metricsCache.putTimelineMetric(metric);
      populateMetricsList(context, sanitizedName);
    }

    private void populateMetricsList(Context context, String... metricNames) {
      for (String metricName : metricNames) {
        TimelineMetric cachedMetric = metricsCache.getTimelineMetric(metricName);
        if (cachedMetric != null) {
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
