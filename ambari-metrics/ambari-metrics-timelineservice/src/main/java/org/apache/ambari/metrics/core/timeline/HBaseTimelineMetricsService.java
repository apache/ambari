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
package org.apache.ambari.metrics.core.timeline;

import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.DEFAULT_INSTANCE_ID;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.USE_GROUPBY_AGGREGATOR_QUERIES;
import static org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.ACTUAL_AGGREGATOR_NAMES;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.metrics.core.timeline.aggregators.Function;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineMetricAggregator;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineMetricAggregatorFactory;
import org.apache.ambari.metrics.core.timeline.availability.MetricCollectorHAController;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricHostMetadata;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.ambari.metrics.core.timeline.function.SeriesAggregateFunction;
import org.apache.ambari.metrics.core.timeline.function.TimelineMetricsSeriesAggregateFunction;
import org.apache.ambari.metrics.core.timeline.function.TimelineMetricsSeriesAggregateFunctionFactory;
import org.apache.ambari.metrics.core.timeline.query.Condition;
import org.apache.ambari.metrics.core.timeline.query.ConditionBuilder;
import org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL;
import org.apache.ambari.metrics.core.timeline.query.TopNCondition;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.metrics2.sink.timeline.AggregationResult;
import org.apache.hadoop.metrics2.sink.timeline.ContainerMetric;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricWithAggregatedValues;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.TopNConfig;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;
import org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class HBaseTimelineMetricsService extends AbstractService implements TimelineMetricStore {

  static final Log LOG = LogFactory.getLog(HBaseTimelineMetricsService.class);
  private final TimelineMetricConfiguration configuration;
  private TimelineMetricDistributedCache cache;
  private PhoenixHBaseAccessor hBaseAccessor;
  private static volatile boolean isInitialized = false;
  private final ScheduledExecutorService watchdogExecutorService = Executors.newSingleThreadScheduledExecutor();
  private final Map<AGGREGATOR_NAME, ScheduledExecutorService> scheduledExecutors = new HashMap<>();
  private TimelineMetricMetadataManager metricMetadataManager;
  private MetricCollectorHAController haController;
  private boolean containerMetricsDisabled = false;

  /**
   * Construct the service.
   */
  public HBaseTimelineMetricsService(TimelineMetricConfiguration configuration) {
    super(HBaseTimelineMetricsService.class.getName());
    this.configuration = configuration;
  }

  @Override
  protected void serviceInit(Configuration conf) throws Exception {
    super.serviceInit(conf);
    initializeSubsystem();
  }

  private TimelineMetricDistributedCache startCacheNode() throws MalformedURLException, URISyntaxException {
    //TODO make configurable
    return new TimelineMetricsIgniteCache(metricMetadataManager);
  }


  private synchronized void initializeSubsystem() {
    if (!isInitialized) {
      hBaseAccessor = new PhoenixHBaseAccessor(null);

      // Initialize metadata
      try {
        metricMetadataManager = new TimelineMetricMetadataManager(hBaseAccessor);
      } catch (MalformedURLException | URISyntaxException e) {
        throw new ExceptionInInitializerError("Unable to initialize metadata manager");
      }
      metricMetadataManager.initializeMetadata();

      // Initialize metric schema
      hBaseAccessor.initMetricSchema();

      // Initialize policies before TTL update
      hBaseAccessor.initPoliciesAndTTL();
      // Start HA service
      // Start the controller
      if (!configuration.isDistributedCollectorModeDisabled()) {
        haController = new MetricCollectorHAController(configuration);
        try {
          haController.initializeHAController();
        } catch (Exception e) {
          LOG.error(e);
          throw new MetricsSystemInitializationException("Unable to " +
            "initialize HA controller", e);
        }
      } else {
        LOG.info("Distributed collector mode disabled");
      }

      //Initialize whitelisting & blacklisting if needed
      TimelineMetricsFilter.initializeMetricFilter(configuration);

      Configuration metricsConf = null;
      try {
        metricsConf = configuration.getMetricsConf();
      } catch (Exception e) {
        throw new ExceptionInInitializerError("Cannot initialize configuration.");
      }

      if (configuration.isCollectorInMemoryAggregationEnabled()) {
        try {
          cache = startCacheNode();
        } catch (Exception e) {
          throw new MetricsSystemInitializationException("Unable to " +
            "start cache node", e);
        }
      }

      if (Boolean.parseBoolean(metricsConf.get(USE_GROUPBY_AGGREGATOR_QUERIES, "true"))) {
        LOG.info("Using group by aggregators for aggregating host and cluster metrics.");
      }

      // Start the cluster aggregator second
      TimelineMetricAggregator secondClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(
          hBaseAccessor, metricsConf, metricMetadataManager, haController, cache);
      scheduleAggregatorThread(secondClusterAggregator);

      // Start the minute cluster aggregator
      TimelineMetricAggregator minuteClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorMinute(
          hBaseAccessor, metricsConf, metricMetadataManager, haController);
      scheduleAggregatorThread(minuteClusterAggregator);

      // Start the hourly cluster aggregator
      TimelineMetricAggregator hourlyClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(
          hBaseAccessor, metricsConf, metricMetadataManager, haController);
      scheduleAggregatorThread(hourlyClusterAggregator);

      // Start the daily cluster aggregator
      TimelineMetricAggregator dailyClusterAggregator =
        TimelineMetricAggregatorFactory.createTimelineClusterAggregatorDaily(
          hBaseAccessor, metricsConf, metricMetadataManager, haController);
      scheduleAggregatorThread(dailyClusterAggregator);

      // Start the minute host aggregator
      if (!configuration.isHostInMemoryAggregationEnabled()) {
        TimelineMetricAggregator minuteHostAggregator =
          TimelineMetricAggregatorFactory.createTimelineMetricAggregatorMinute(
            hBaseAccessor, metricsConf, metricMetadataManager, haController);
        scheduleAggregatorThread(minuteHostAggregator);
      }

      // Start the hourly host aggregator
      TimelineMetricAggregator hourlyHostAggregator =
        TimelineMetricAggregatorFactory.createTimelineMetricAggregatorHourly(
          hBaseAccessor, metricsConf, metricMetadataManager, haController);
      scheduleAggregatorThread(hourlyHostAggregator);

      // Start the daily host aggregator
      TimelineMetricAggregator dailyHostAggregator =
        TimelineMetricAggregatorFactory.createTimelineMetricAggregatorDaily(
          hBaseAccessor, metricsConf, metricMetadataManager, haController);
      scheduleAggregatorThread(dailyHostAggregator);

      if (!configuration.isTimelineMetricsServiceWatcherDisabled()) {
        int initDelay = configuration.getTimelineMetricsServiceWatcherInitDelay();
        int delay = configuration.getTimelineMetricsServiceWatcherDelay();
        // Start the watchdog
        watchdogExecutorService.scheduleWithFixedDelay(
          new TimelineMetricStoreWatcher(this, configuration),
          initDelay, delay, TimeUnit.SECONDS);
        LOG.info("Started watchdog for timeline metrics store with initial " +
          "delay = " + initDelay + ", delay = " + delay);
      }
      containerMetricsDisabled = configuration.isContainerMetricsDisabled();
      isInitialized = true;
    }

  }

  @Override
  protected void serviceStop() throws Exception {
    super.serviceStop();
  }

  @Override
  public TimelineMetrics getTimelineMetrics(List<String> metricNames,
                                            List<String> hostnames, String applicationId, String instanceId,
                                            Long startTime, Long endTime, Precision precision, Integer limit,
                                            boolean groupedByHosts, TopNConfig topNConfig, String seriesAggregateFunction) throws SQLException, IOException {

    if (metricNames == null || metricNames.isEmpty()) {
      throw new IllegalArgumentException("No metric name filter specified.");
    }
    if ((startTime == null && endTime != null)
      || (startTime != null && endTime == null)) {
      throw new IllegalArgumentException("Open ended query not supported ");
    }
    if (limit != null && limit > PhoenixHBaseAccessor.RESULTSET_LIMIT) {
      throw new IllegalArgumentException("Limit too big");
    }

    TimelineMetricsSeriesAggregateFunction seriesAggrFunctionInstance = null;
    if (!StringUtils.isEmpty(seriesAggregateFunction)) {
      SeriesAggregateFunction func = SeriesAggregateFunction.getFunction(seriesAggregateFunction);
      seriesAggrFunctionInstance = TimelineMetricsSeriesAggregateFunctionFactory.newInstance(func);
    }

    Multimap<String, List<Function>> metricFunctions =
      parseMetricNamesToAggregationFunctions(metricNames);

    TimelineMetrics metrics = new TimelineMetrics();
    List<String> transientMetricNames = new ArrayList<>();

    if (configuration.getTimelineMetricsMultipleClusterSupport() && StringUtils.isEmpty(instanceId)) {
      instanceId = DEFAULT_INSTANCE_ID;
    }

    List<byte[]> uuids = metricMetadataManager.getUuidsForGetMetricQuery(metricFunctions.keySet(),
      hostnames,
      applicationId,
      instanceId,
      transientMetricNames);

    if (uuids.isEmpty() && transientMetricNames.isEmpty()) {
      LOG.trace("No metrics satisfy the query: " + Arrays.asList(metricNames).toString());
      return metrics;
    }

    ConditionBuilder conditionBuilder = new ConditionBuilder(new ArrayList<String>(metricFunctions.keySet()))
      .hostnames(hostnames)
      .appId(applicationId)
      .instanceId(instanceId)
      .startTime(startTime)
      .endTime(endTime)
      .precision(precision)
      .limit(limit)
      .grouped(groupedByHosts)
      .uuid(uuids)
      .transientMetricNames(transientMetricNames);

    applyTopNCondition(conditionBuilder, topNConfig, metricNames, hostnames);

    Condition condition = conditionBuilder.build();

    if (CollectionUtils.isEmpty(hostnames)) {
      metrics = hBaseAccessor.getAggregateMetricRecords(condition, metricFunctions);
    } else {
      metrics = hBaseAccessor.getMetricRecords(condition, metricFunctions);
    }

    metrics = postProcessMetrics(metrics);

    if (metrics.getMetrics().size() == 0) {
      return metrics;
    }

    return seriesAggregateMetrics(seriesAggrFunctionInstance, metrics);
  }

  private void applyTopNCondition(ConditionBuilder conditionBuilder,
                                  TopNConfig topNConfig,
                                  List<String> metricNames,
                                  List<String> hostnames) {
    if (topNConfig != null) {
      if (TopNCondition.isTopNHostCondition(metricNames, hostnames) ^ //Only 1 condition should be true.
        TopNCondition.isTopNMetricCondition(metricNames, hostnames)) {
        conditionBuilder.topN(topNConfig.getTopN());
        conditionBuilder.isBottomN(topNConfig.getIsBottomN());
        Function.ReadFunction readFunction = Function.ReadFunction.getFunction(topNConfig.getTopNFunction());
        Function function = new Function(readFunction, null);
        conditionBuilder.topNFunction(function);
      } else {
        LOG.debug("Invalid Input for TopN query. Ignoring TopN Request.");
      }
    }
  }

  private TimelineMetrics postProcessMetrics(TimelineMetrics metrics) {
    List<TimelineMetric> metricsList = metrics.getMetrics();

    for (TimelineMetric metric : metricsList) {
      String name = metric.getMetricName();
      if (name.contains("._rate")) {
        updateValuesAsRate(metric.getMetricValues(), false);
      } else if (name.contains("._diff")) {
        updateValuesAsRate(metric.getMetricValues(), true);
      }
    }

    return metrics;
  }

  private TimelineMetrics seriesAggregateMetrics(TimelineMetricsSeriesAggregateFunction seriesAggrFuncInstance,
                                                 TimelineMetrics metrics) {
    if (seriesAggrFuncInstance != null) {
      TimelineMetric appliedMetric = seriesAggrFuncInstance.apply(metrics);
      metrics.setMetrics(Collections.singletonList(appliedMetric));
    }
    return metrics;
  }

  static Map<Long, Double> updateValuesAsRate(Map<Long, Double> metricValues, boolean isDiff) {
    Long prevTime = null;
    Double prevVal = null;
    long step;
    Double diff;

    for (Iterator<Map.Entry<Long, Double>> it = metricValues.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<Long, Double> timeValueEntry = it.next();
      Long currTime = timeValueEntry.getKey();
      Double currVal = timeValueEntry.getValue();

      if (prevTime != null) {
        step = currTime - prevTime;
        diff = currVal - prevVal;
        if (diff < 0) {
          it.remove(); //Discard calculating rate when the metric counter has been reset.
        } else {
          Double rate = isDiff ? diff : (diff / TimeUnit.MILLISECONDS.toSeconds(step));
          timeValueEntry.setValue(rate);
        }
      } else {
        it.remove();
      }

      prevTime = currTime;
      prevVal = currVal;
    }

    return metricValues;
  }

  static Multimap<String, List<Function>> parseMetricNamesToAggregationFunctions(List<String> metricNames) {
    Multimap<String, List<Function>> metricsFunctions = ArrayListMultimap.create();

    for (String metricName : metricNames) {
      Function function = Function.DEFAULT_VALUE_FUNCTION;
      String cleanMetricName = metricName;

      try {
        function = Function.fromMetricName(metricName);
        int functionStartIndex = metricName.indexOf("._");
        if (functionStartIndex > 0) {
          cleanMetricName = metricName.substring(0, functionStartIndex);
        }
      } catch (Function.FunctionFormatException ffe) {
        // unknown function so
        // fallback to VALUE, and fullMetricName
      }

      List<Function> functionsList = new ArrayList<>();
      functionsList.add(function);
      metricsFunctions.put(cleanMetricName, functionsList);
    }

    return metricsFunctions;
  }

  public TimelinePutResponse putMetricsSkipCache(TimelineMetrics metrics) throws SQLException, IOException {
    TimelinePutResponse response = new TimelinePutResponse();
    hBaseAccessor.insertMetricRecordsWithMetadata(metricMetadataManager, metrics, true);
    return response;
  }

  @Override
  public TimelinePutResponse putMetrics(TimelineMetrics metrics) throws SQLException, IOException {
    // Error indicated by the Sql exception
    TimelinePutResponse response = new TimelinePutResponse();

    hBaseAccessor.insertMetricRecordsWithMetadata(metricMetadataManager, metrics, false);

    if (configuration.isCollectorInMemoryAggregationEnabled()) {
      cache.putMetrics(metrics.getMetrics());
    }

    return response;
  }

  @Override
  public TimelinePutResponse putContainerMetrics(List<ContainerMetric> metrics)
    throws SQLException, IOException {

    if (containerMetricsDisabled) {
      LOG.debug("Ignoring submitted container metrics according to configuration. Values will not be stored.");
      return new TimelinePutResponse();
    }

    hBaseAccessor.insertContainerMetrics(metrics);
    return new TimelinePutResponse();
  }

  @Override
  public Map<String, List<TimelineMetricMetadata>> getTimelineMetricMetadata(String appId, String metricPattern,
                                                                             boolean includeBlacklistedMetrics) throws SQLException, IOException {
    return metricMetadataManager.getTimelineMetricMetadataByAppId(appId, metricPattern, includeBlacklistedMetrics);
  }

  @Override
  public byte[] getUuid(String metricName, String appId, String instanceId, String hostname) throws SQLException, IOException {
    return metricMetadataManager.getUuid(metricName, appId, instanceId, hostname, false);
  }

  @Override
  public Map<String, Set<String>> getHostAppsMetadata() throws SQLException, IOException {
    Map<String, TimelineMetricHostMetadata> hostsMetadata = metricMetadataManager.getHostedAppsCache();
    Map<String, Set<String>> hostAppMap = new HashMap<>();
    for (String hostname : hostsMetadata.keySet()) {
      hostAppMap.put(hostname, hostsMetadata.get(hostname).getHostedApps().keySet());
    }
    return hostAppMap;
  }

  @Override
  public TimelinePutResponse putHostAggregatedMetrics(AggregationResult aggregationResult) throws SQLException, IOException {
    Map<TimelineMetric, MetricHostAggregate> aggregateMap = new HashMap<>();
    String hostname = null;
    for (TimelineMetricWithAggregatedValues entry : aggregationResult.getResult()) {
      aggregateMap.put(entry.getTimelineMetric(), entry.getMetricAggregate());
      hostname = hostname == null ? entry.getTimelineMetric().getHostName() : hostname;
    }
    long timestamp = aggregationResult.getTimeInMilis();
    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("Adding host %s to aggregated by in-memory aggregator. Timestamp : %s", hostname, timestamp));
    }
    hBaseAccessor.saveHostAggregateRecords(aggregateMap, PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME);

    return new TimelinePutResponse();
  }

  @Override
  public Map<String, Map<String, Set<String>>> getInstanceHostsMetadata(String instanceId, String appId)
    throws SQLException, IOException {

    Map<String, TimelineMetricHostMetadata> hostedApps = metricMetadataManager.getHostedAppsCache();
    Map<String, Set<String>> instanceHosts = new HashMap<>();
    if (configuration.getTimelineMetricsMultipleClusterSupport()) {
      instanceHosts = metricMetadataManager.getHostedInstanceCache();
    }

    Map<String, Map<String, Set<String>>> instanceAppHosts = new HashMap<>();

    if (MapUtils.isEmpty(instanceHosts)) {
      Map<String, Set<String>> appHostMap = new HashMap<String, Set<String>>();
      for (String host : hostedApps.keySet()) {
        for (String app : hostedApps.get(host).getHostedApps().keySet()) {
          if (!appHostMap.containsKey(app)) {
            appHostMap.put(app, new HashSet<String>());
          }
          appHostMap.get(app).add(host);
        }
      }
      instanceAppHosts.put("", appHostMap);
    } else {
      for (String instance : instanceHosts.keySet()) {

        if (StringUtils.isNotEmpty(instanceId) && !instance.equals(instanceId)) {
          continue;
        }
        Map<String, Set<String>> appHostMap = new HashMap<String, Set<String>>();
        instanceAppHosts.put(instance, appHostMap);

        Set<String> hostsWithInstance = instanceHosts.get(instance);
        for (String host : hostsWithInstance) {
          for (String app : hostedApps.get(host).getHostedApps().keySet()) {
            if (StringUtils.isNotEmpty(appId) && !app.equals(appId)) {
              continue;
            }

            if (!appHostMap.containsKey(app)) {
              appHostMap.put(app, new HashSet<String>());
            }
            appHostMap.get(app).add(host);
          }
        }
      }
    }

    return instanceAppHosts;
  }

  @Override
  public List<String> getLiveInstances() {

    List<String> instances = null;
    try {
      if (haController == null) {
        // Always return current host as live (embedded operation mode)
        return Collections.singletonList(configuration.getInstanceHostnameFromEnv());
      }
      instances = haController.getLiveInstanceHostNames();
      if (instances == null || instances.isEmpty()) {
        // fallback
        instances = Collections.singletonList(configuration.getInstanceHostnameFromEnv());
      }
    } catch (UnknownHostException e) {
      LOG.debug("Exception on getting hostname from env.", e);
    }
    return instances;
  }

  @Override
  public TimelineMetricServiceSummary getTimelineMetricServiceSummary() {
    return new TimelineMetricServiceSummary(metricMetadataManager, haController);
  }

  private void scheduleAggregatorThread(final TimelineMetricAggregator aggregator) {
    if (!aggregator.isDisabled()) {
      ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactory() {
          @Override
          public Thread newThread(Runnable r) {
            return new Thread(r, ACTUAL_AGGREGATOR_NAMES.get(aggregator.getName()));
          }
        }
      );
      scheduledExecutors.put(aggregator.getName(), executorService);
      executorService.scheduleAtFixedRate(aggregator,
        0l,
        aggregator.getSleepIntervalMillis(),
        TimeUnit.MILLISECONDS);
      LOG.info("Scheduled aggregator thread " + aggregator.getName() + " every " +
        + aggregator.getSleepIntervalMillis() + " milliseconds.");
    } else {
      LOG.info("Skipped scheduling " + aggregator.getName() + " since it is disabled.");
    }
  }
}
