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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.DEFAULT_TOPN_HOSTS_LIMIT;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.USE_GROUPBY_AGGREGATOR_QUERIES;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.ACTUAL_AGGREGATOR_NAMES;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregatorFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricHostMetadata;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataKey;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.function.SeriesAggregateFunction;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.function.TimelineMetricsSeriesAggregateFunction;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.function.TimelineMetricsSeriesAggregateFunctionFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.ConditionBuilder;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.TopNCondition;

public class HBaseTimelineMetricsService extends AbstractService implements TimelineMetricStore {

  static final Log LOG = LogFactory.getLog(HBaseTimelineMetricsService.class);
  private final TimelineMetricConfiguration configuration;
  private TimelineMetricDistributedCache cache;
  private PhoenixHBaseAccessor hBaseAccessor;
  private static volatile boolean isInitialized = false;
  private final ScheduledExecutorService watchdogExecutorService = Executors.newSingleThreadScheduledExecutor();
  private final Map<AGGREGATOR_NAME, ScheduledExecutorService> scheduledExecutors = new HashMap<>();
  private final ConcurrentHashMap<String, Long> postedAggregatedMap = new ConcurrentHashMap<>();
  private TimelineMetricMetadataManager metricMetadataManager;
  private Integer defaultTopNHostsLimit;
  private MetricCollectorHAController haController;
  private boolean containerMetricsDisabled = false;

  /**
   * Construct the service.
   *
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
    return new TimelineMetricsIgniteCache();
  }


  private synchronized void initializeSubsystem() {
    if (!isInitialized) {
      hBaseAccessor = new PhoenixHBaseAccessor(null);
      // Initialize schema
      hBaseAccessor.initMetricSchema();
      // Initialize metadata from store
      try {
        metricMetadataManager = new TimelineMetricMetadataManager(hBaseAccessor);
      } catch (MalformedURLException | URISyntaxException e) {
        throw new ExceptionInInitializerError("Unable to initialize metadata manager");
      }
      metricMetadataManager.initializeMetadata();
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

      defaultTopNHostsLimit = Integer.parseInt(metricsConf.get(DEFAULT_TOPN_HOSTS_LIMIT, "20"));
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
      if (configuration.isHostInMemoryAggregationEnabled()) {
        LOG.info("timeline.metrics.host.inmemory.aggregation is set to True, switching to filtering host minute aggregation on collector");
        TimelineMetricAggregator minuteHostAggregator =
          TimelineMetricAggregatorFactory.createFilteringTimelineMetricAggregatorMinute(
            hBaseAccessor, metricsConf, metricMetadataManager, haController, postedAggregatedMap);
        scheduleAggregatorThread(minuteHostAggregator);
      } else {
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
    if (limit != null && limit > PhoenixHBaseAccessor.RESULTSET_LIMIT){
      throw new IllegalArgumentException("Limit too big");
    }

    TimelineMetricsSeriesAggregateFunction seriesAggrFunctionInstance = null;
    if (!StringUtils.isEmpty(seriesAggregateFunction)) {
      SeriesAggregateFunction func = SeriesAggregateFunction.getFunction(seriesAggregateFunction);
      seriesAggrFunctionInstance = TimelineMetricsSeriesAggregateFunctionFactory.newInstance(func);
    }

    Multimap<String, List<Function>> metricFunctions =
      parseMetricNamesToAggregationFunctions(metricNames);

    List<byte[]> uuids = metricMetadataManager.getUuids(metricFunctions.keySet(), hostnames, applicationId, instanceId);

    ConditionBuilder conditionBuilder = new ConditionBuilder(new ArrayList<String>(metricFunctions.keySet()))
      .hostnames(hostnames)
      .appId(applicationId)
      .instanceId(instanceId)
      .startTime(startTime)
      .endTime(endTime)
      .precision(precision)
      .limit(limit)
      .grouped(groupedByHosts)
      .uuid(uuids);

    if (topNConfig != null) {
      if (TopNCondition.isTopNHostCondition(metricNames, hostnames) ^ //Only 1 condition should be true.
        TopNCondition.isTopNMetricCondition(metricNames, hostnames)) {
        conditionBuilder.topN(topNConfig.getTopN());
        conditionBuilder.isBottomN(topNConfig.getIsBottomN());
        Function.ReadFunction readFunction = Function.ReadFunction.getFunction(topNConfig.getTopNFunction());
        Function function = new Function(readFunction, null);
        conditionBuilder.topNFunction(function);
      } else {
        LOG.info("Invalid Input for TopN query. Ignoring TopN Request.");
      }
    } else if (startTime != null && hostnames != null && hostnames.size() > defaultTopNHostsLimit) {
      // if (timeseries query AND hostnames passed AND size(hostnames) > limit)
      LOG.info("Requesting data for more than " + defaultTopNHostsLimit +  " Hosts. " +
        "Defaulting to Top " + defaultTopNHostsLimit);
      conditionBuilder.topN(defaultTopNHostsLimit);
      conditionBuilder.isBottomN(false);
    }

    Condition condition = conditionBuilder.build();

    TimelineMetrics metrics;

    if (hostnames == null || hostnames.isEmpty()) {
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

  private TimelineMetrics postProcessMetrics(TimelineMetrics metrics) {
    List<TimelineMetric> metricsList = metrics.getMetrics();

    for (TimelineMetric metric : metricsList){
      String name = metric.getMetricName();
      if (name.contains("._rate")){
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

    for(Iterator<Map.Entry<Long, Double>> it = metricValues.entrySet().iterator(); it.hasNext(); ) {
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

    for (String metricName : metricNames){
      Function function = Function.DEFAULT_VALUE_FUNCTION;
      String cleanMetricName = metricName;

      try {
        function = Function.fromMetricName(metricName);
        int functionStartIndex = metricName.indexOf("._");
        if (functionStartIndex > 0) {
          cleanMetricName = metricName.substring(0, functionStartIndex);
        }
      } catch (Function.FunctionFormatException ffe){
        // unknown function so
        // fallback to VALUE, and fullMetricName
      }

      List<Function>  functionsList = new ArrayList<>();
      functionsList.add(function);
      metricsFunctions.put(cleanMetricName, functionsList);
    }

    return metricsFunctions;
  }

  @Override
  public TimelinePutResponse putMetrics(TimelineMetrics metrics) throws SQLException, IOException {
    // Error indicated by the Sql exception
    TimelinePutResponse response = new TimelinePutResponse();

    hBaseAccessor.insertMetricRecordsWithMetadata(metricMetadataManager, metrics, false);

    if (configuration.isCollectorInMemoryAggregationEnabled()) {
      cache.putMetrics(metrics.getMetrics(), metricMetadataManager);
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
    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadata =
      metricMetadataManager.getMetadataCache();

    boolean filterByAppId = StringUtils.isNotEmpty(appId);
    boolean filterByMetricName = StringUtils.isNotEmpty(metricPattern);
    Pattern metricFilterPattern = null;
    if (filterByMetricName) {
      metricFilterPattern = Pattern.compile(metricPattern);
    }

    // Group Metadata by AppId
    Map<String, List<TimelineMetricMetadata>> metadataByAppId = new HashMap<>();
    for (TimelineMetricMetadata metricMetadata : metadata.values()) {

      if (!includeBlacklistedMetrics && !metricMetadata.isWhitelisted()) {
        continue;
      }

      String currentAppId = metricMetadata.getAppId();
      if (filterByAppId && !currentAppId.equals(appId)) {
        continue;
      }

      if (filterByMetricName) {
        Matcher m = metricFilterPattern.matcher(metricMetadata.getMetricName());
        if (!m.find()) {
          continue;
        }
      }

      List<TimelineMetricMetadata> metadataList = metadataByAppId.get(currentAppId);
      if (metadataList == null) {
        metadataList = new ArrayList<>();
        metadataByAppId.put(currentAppId, metadataList);
      }

      metadataList.add(metricMetadata);
    }

    return metadataByAppId;
  }

  @Override
  public byte[] getUuid(String metricName, String appId, String instanceId, String hostname) throws SQLException, IOException {
    return metricMetadataManager.getUuid(metricName, appId, instanceId, hostname);
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
      break;
    }
    long timestamp = aggregationResult.getTimeInMilis();
    postedAggregatedMap.put(hostname, timestamp);
    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("Adding host %s to aggregated by in-memory aggregator. Timestamp : %s", hostname, timestamp));
    }
    hBaseAccessor.saveHostAggregateRecords(aggregateMap, PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME);


    return new TimelinePutResponse();
  }

  @Override
  public Map<String, Map<String,Set<String>>> getInstanceHostsMetadata(String instanceId, String appId)
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
        Map<String, Set<String>> appHostMap = new  HashMap<String, Set<String>>();
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
