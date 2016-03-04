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
package org.apache.ambari.server.controller.metrics.timeline;

import com.google.common.collect.Sets;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.MetricsPropertyProvider;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineAppMetricCacheKey;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCache;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.ambari.server.Role.HBASE_MASTER;
import static org.apache.ambari.server.Role.HBASE_REGIONSERVER;
import static org.apache.ambari.server.Role.METRICS_COLLECTOR;
import static org.apache.ambari.server.controller.metrics.MetricsPaddingMethod.ZERO_PADDING_PARAM;
import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.TIMELINE_METRICS;

public abstract class AMSPropertyProvider extends MetricsPropertyProvider {
  private static final String METRIC_REGEXP_PATTERN = "\\([^)]*\\)";
  private static final int COLLECTOR_DEFAULT_PORT = 6188;
  private final TimelineMetricCache metricCache;
  private static AtomicInteger printSkipPopulateMsgHostCounter = new AtomicInteger(0);
  private static AtomicInteger printSkipPopulateMsgHostCompCounter = new AtomicInteger(0);
  private static final Map<String, String> timelineAppIdCache = new ConcurrentHashMap<>(10);

  private static final Map<String, String> JVM_PROCESS_NAMES = new HashMap<>(2);

  static {
    JVM_PROCESS_NAMES.put("HBASE_MASTER", "Master.");
    JVM_PROCESS_NAMES.put("HBASE_REGIONSERVER", "RegionServer.");
  }

  public AMSPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                             URLStreamProvider streamProvider,
                             ComponentSSLConfiguration configuration,
                             TimelineMetricCacheProvider cacheProvider,
                             MetricHostProvider hostProvider,
                             String clusterNamePropertyId,
                             String hostNamePropertyId,
                             String componentNamePropertyId) {

    super(componentPropertyInfoMap, streamProvider, configuration,
      hostProvider, clusterNamePropertyId, hostNamePropertyId,
      componentNamePropertyId);

    this.metricCache = cacheProvider.getTimelineMetricsCache();
  }

  protected String getOverridenComponentName(Resource resource) {
    String componentName = getComponentName(resource);
    // Hack: To allow host queries to succeed
    if (componentName.equals("HOST")) {
      return  "*";
    }
    return componentName;
  }

  /**
   * Support properties with aggregate functions and metrics padding method.
   */
  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> supportedIds = new HashSet<String>();
    for (String propertyId : propertyIds) {
      if (propertyId.startsWith(ZERO_PADDING_PARAM)
          || PropertyHelper.hasAggregateFunctionSuffix(propertyId)) {
        supportedIds.add(propertyId);
      }
    }
    propertyIds.removeAll(supportedIds);
    return propertyIds;
  }

  /**
   * The information required to make a single call to the Metrics service.
   */
  class MetricsRequest {
    private final TemporalInfo temporalInfo;
    private final Map<String, Set<Resource>> resources = new HashMap<String, Set<Resource>>();
    private final Map<String, Set<String>> metrics = new HashMap<String, Set<String>>();
    private final URIBuilder uriBuilder;
    Set<String> resolvedMetricsParams;
    MetricsRequestHelper requestHelper = new MetricsRequestHelper(streamProvider);

    // Metrics with amsHostMetric = true
    // Basically a host metric to be returned for a hostcomponent
    private final Set<String> hostComponentHostMetrics = new HashSet<String>();
    private String clusterName;

    private MetricsRequest(TemporalInfo temporalInfo, URIBuilder uriBuilder,
                           String clusterName) {
      this.temporalInfo = temporalInfo;
      this.uriBuilder = uriBuilder;
      this.clusterName = clusterName;
    }

    public String getClusterName() {
      return clusterName;
    }

    public void putResource(String componentName, Resource resource) {
      Set<Resource> resourceSet = resources.get(componentName);
      if (resourceSet == null) {
        resourceSet = new HashSet<Resource>();
        resources.put(componentName, resourceSet);
      }
      resourceSet.add(resource);
    }

    public void putPropertyId(String metric, String id) {
      Set<String> propertyIds = metrics.get(metric);

      if (propertyIds == null) {
        propertyIds = new HashSet<String>();
        metrics.put(metric, propertyIds);
      }
      propertyIds.add(id);
    }

    public void putHosComponentHostMetric(String metric) {
      if (metric != null) {
        hostComponentHostMetrics.add(metric);
      }
    }

    private TimelineMetrics getTimelineMetricsFromCache(TimelineAppMetricCacheKey metricCacheKey,
          String componentName) throws IOException {
      // Cache only the component level metrics
      // No point in time metrics are cached
      if (metricCache != null
          && !StringUtils.isEmpty(componentName)
          && !componentName.equalsIgnoreCase("HOST")
          && metricCacheKey.getTemporalInfo() != null) {
        return metricCache.getAppTimelineMetricsFromCache(metricCacheKey);
      }

      Long startTime = (metricCacheKey.getTemporalInfo() != null) ? metricCacheKey.getTemporalInfo().getStartTimeMillis():null;
      Long endTime = (metricCacheKey.getTemporalInfo() != null) ? metricCacheKey.getTemporalInfo().getEndTimeMillis():null;

      return requestHelper.fetchTimelineMetrics(uriBuilder, startTime, endTime);
    }



    /**
     * Populate the associated resources by making a call to the Metrics
     * service.
     *
     * @return a collection of populated resources
     * @throws SystemException if unable to populate the resources
     */
    @SuppressWarnings("unchecked")
    public Collection<Resource> populateResources() throws SystemException {
      // No open ended query support.
      if (temporalInfo != null && (temporalInfo.getStartTime() == null
          || temporalInfo.getEndTime() == null)) {
        return Collections.emptySet();
      }

      for (Map.Entry<String, Set<Resource>> resourceEntry : resources.entrySet()) {
        String componentName = resourceEntry.getKey();
        Set<Resource> resourceSet = resourceEntry.getValue();

        TimelineMetrics timelineMetrics = new TimelineMetrics();

        Set<String> nonHostComponentMetrics = new HashSet<String>(metrics.keySet());
        nonHostComponentMetrics.removeAll(hostComponentHostMetrics);
        String hostnames = getHostnames(resources.get(componentName));

        // Allow for multiple requests since host metrics for a
        // hostcomponent need the HOST appId
        if (!hostComponentHostMetrics.isEmpty()) {
          String hostComponentHostMetricParams = getSetString(processRegexps(hostComponentHostMetrics), -1);
          setQueryParams(hostComponentHostMetricParams, hostnames, true, componentName);
          TimelineMetrics metricsResponse = null;
          try {
            metricsResponse = getTimelineMetricsFromCache(
              getTimelineAppMetricCacheKey(hostComponentHostMetrics,
                componentName, hostnames, uriBuilder.toString()), componentName);
          } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Caught exception fetching metric data.", e);
            }
            // Skip further queries to preempt long calls due to timeout
            if (e instanceof SocketTimeoutException) {
              return Collections.emptySet();
            }
          }
          if (metricsResponse != null) {
            timelineMetrics.getMetrics().addAll(metricsResponse.getMetrics());
          }
        }

        if (!nonHostComponentMetrics.isEmpty()) {
          String nonHostComponentHostMetricParams = getSetString(processRegexps(nonHostComponentMetrics), -1);
          setQueryParams(nonHostComponentHostMetricParams, hostnames, false, componentName);
          TimelineMetrics metricsResponse = null;
          try {
            metricsResponse = getTimelineMetricsFromCache(
              getTimelineAppMetricCacheKey(nonHostComponentMetrics,
                componentName, hostnames, uriBuilder.toString()), componentName);
          } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Caught exception fetching metric data.", e);
            }
            // Skip further queries to preempt long calls due to timeout
            if (e instanceof SocketTimeoutException) {
              return Collections.emptySet();
            }
          }
          if (metricsResponse != null) {
            timelineMetrics.getMetrics().addAll(metricsResponse.getMetrics());
          }
        }

        Map<String, Set<TimelineMetric>> metricsMap = new HashMap<String, Set<TimelineMetric>>();
        Set<String> patterns = createPatterns(metrics.keySet());

        if (!timelineMetrics.getMetrics().isEmpty()) {
          for (TimelineMetric metric : timelineMetrics.getMetrics()) {
            if (metric.getMetricName() != null
                && metric.getMetricValues() != null
                && checkMetricName(patterns, metric.getMetricName())) {
              String hostname = metric.getHostName();
              if (!metricsMap.containsKey(hostname)) {
                metricsMap.put(hostname, new HashSet<TimelineMetric>());
              }
              metricsMap.get(hostname).add(metric);
            }
          }
          for (Resource resource : resourceSet) {
            String hostname = getHostName(resource);
            if (metricsMap.containsKey(hostname)) {
              for (TimelineMetric metric : metricsMap.get(hostname)) {
                // Pad zeros or nulls if needed to a clone so we do not cache
                // padded values
                TimelineMetric timelineMetricClone = new TimelineMetric(metric);
                metricsPaddingMethod.applyPaddingStrategy(timelineMetricClone, temporalInfo);
                populateResource(resource, timelineMetricClone, temporalInfo);
              }
            }
          }
        }
      }

      return Collections.emptySet();
    }

    private String getTimelineAppId(String componentName) {
      if (timelineAppIdCache.containsKey(componentName)) {
        return timelineAppIdCache.get(componentName);
      } else {
        StackId stackId;
        try {
          AmbariManagementController managementController = AmbariServer.getController();
          stackId = managementController.getClusters().getCluster(clusterName).getCurrentStackVersion();
          if (stackId != null) {
            String stackName = stackId.getStackName();
            String version = stackId.getStackVersion();
            AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();
            String serviceName = ambariMetaInfo.getComponentToService(stackName, version, componentName);
            String timeLineAppId = ambariMetaInfo.getComponent(stackName, version, serviceName, componentName).getTimelineAppid();
            if (timeLineAppId != null){
              timelineAppIdCache.put(componentName, timeLineAppId);
              return timeLineAppId;
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      return componentName;
    }

    private void setQueryParams(String metricsParam, String hostname,
                                boolean isHostMetric, String componentName) {
      // Reuse uriBuilder
      uriBuilder.removeQuery();

      if (metricsParam.length() > 0) {
        uriBuilder.setParameter("metricNames", metricsParam);
        resolvedMetricsParams = Sets.newHashSet(metricsParam.split(","));
      }

      if (hostname != null && !hostname.isEmpty()) {
        uriBuilder.setParameter("hostname", hostname);
      }

      if (isHostMetric) {
        uriBuilder.setParameter("appId", "HOST");
      } else {
        if (componentName != null && !componentName.isEmpty()
            && !componentName.equalsIgnoreCase("HOST")) {
          componentName = getTimelineAppId(componentName);
        }
        uriBuilder.setParameter("appId", componentName);
      }

      if (temporalInfo != null) {
        long startTime = temporalInfo.getStartTime();
        if (startTime != -1) {
          uriBuilder.setParameter("startTime", String.valueOf(startTime));
        }

        long endTime = temporalInfo.getEndTime();
        if (endTime != -1) {
          uriBuilder.setParameter("endTime", String.valueOf(endTime));
        }
      }
    }

    private Set<String> createPatterns(Set<String> rawNames) {
      Pattern pattern = Pattern.compile(METRIC_REGEXP_PATTERN);
      Set<String> result = new HashSet<String>();
      for (String rawName : rawNames) {
        Matcher matcher = pattern.matcher(rawName);
        StringBuilder sb = new StringBuilder();
        int lastPos = 0;
        while (matcher.find()) {
          sb.append(Pattern.quote(rawName.substring(lastPos, matcher.start())));
          sb.append(matcher.group());
          lastPos = matcher.end();
        }
        sb.append(Pattern.quote(rawName.substring(lastPos)));
        result.add(sb.toString());
      }
      return result;
    }

    private boolean checkMetricName(Set<String> patterns, String name) {
      for (String pattern : patterns) {
        if (Pattern.matches(pattern, name)) {
          return true;
        }
      }
      return false;
    }

    private Set<String> processRegexps(Set<String> metricNames) {
      Set<String> result = new HashSet<String>();
      for (String name : metricNames) {
        result.add(name.replaceAll(METRIC_REGEXP_PATTERN, Matcher.quoteReplacement("%")));
      }
      return result;
    }

    private void populateResource(Resource resource, TimelineMetric metric,
                                  TemporalInfo temporalInfo) {
      String metric_name = metric.getMetricName();
      Set<String> propertyIdSet = metrics.get(metric_name);
      List<String> parameterList  = new LinkedList<String>();

      if (propertyIdSet == null) {
        for (Map.Entry<String, Set<String>> entry : metrics.entrySet()) {
          String key = entry.getKey();
          Pattern pattern = Pattern.compile(key);
          Matcher matcher = pattern.matcher(metric_name);

          if (matcher.matches()) {
            propertyIdSet = entry.getValue();
            // get parameters
            for (int i = 0; i < matcher.groupCount(); ++i) {
              parameterList.add(matcher.group(i + 1));
            }
            break;
          }
        }
      }
      if (propertyIdSet != null) {
        Map<String, PropertyInfo> metricsMap = getComponentMetrics().get(getOverridenComponentName(resource));
        if (metricsMap != null) {
          for (String propertyId : propertyIdSet) {
            if (propertyId != null) {
//              propertyId = postProcessPropertyId(propertyId, getComponentName(resource));
              if (metricsMap.containsKey(propertyId)){
                if (containsArguments(propertyId)) {
                  int i = 1;
                  //if nothing to substitute in metric name, then
                  //substitute $1 with an instanceId
                  if (!parameterList.isEmpty())  {
                    for (String param : parameterList) {
                      propertyId = substituteArgument(propertyId, "$" + i, param);
                      ++i;
                    }
                  } else {
                    propertyId = substituteArgument(propertyId, "$1", metric.getInstanceId());
                  }
                }
                Object value = getValue(metric, temporalInfo);
                if (value != null && !containsArguments(propertyId)) {
                  resource.setProperty(propertyId, value);
                }
              }
            }
          }
        }
      }
    }

    // Called when host component metrics are present
    private TimelineAppMetricCacheKey getTimelineAppMetricCacheKey(Set<String> metrics,
        String hostnames, String componentName, String spec) {

      TimelineAppMetricCacheKey metricCacheKey =
        new TimelineAppMetricCacheKey(metrics, componentName, hostnames, temporalInfo);

      // Set Uri on the cache key so the only job of the cache update is
      // tweaking the params. Note: Passing UriBuilder reference is unsafe
      // due to reuse. Also, the Uri can only be constructed with a resource
      // request which ties it to the cluster.
      metricCacheKey.setSpec(spec);

      return metricCacheKey;
    }
  }

  private String getHostnames(Set<Resource> resources) {
    StringBuilder hostnames = new StringBuilder();
    for (Resource resource: resources) {
      String hostname = getHostName(resource);
      if (hostname == null) {
        break;
      }
      if (hostnames.length() > 0) {
        hostnames.append(',');
      }
      hostnames.append(hostname);
    }
    return hostnames.toString();
  }

  @Override
  public Set<Resource> populateResourcesWithProperties(Set<Resource> resources,
               Request request, Set<String> propertyIds) throws SystemException {

    Map<String, Map<TemporalInfo, MetricsRequest>> requestMap = getMetricsRequests(resources, request, propertyIds);

    // For each cluster
    for (Map.Entry<String, Map<TemporalInfo, MetricsRequest>> clusterEntry : requestMap.entrySet()) {
      // For each request
      for (MetricsRequest metricsRequest : clusterEntry.getValue().values() ) {
        metricsRequest.populateResources();
      }
    }

    return resources;
  }

  /**
   * Return a propertyInfoMap for all metrics. Handles special case for
   * METRICS_COLLECTOR metrics by returning HBase metrics.
   */
  @Override
  public Map<String, Map<String, PropertyInfo>> getComponentMetrics() {
    if (super.getComponentMetrics().containsKey(METRICS_COLLECTOR.name())) {
      return super.getComponentMetrics();
    }

    Map<String, Map<String, PropertyInfo>> metricPropertyIds;
    if (this.hostNamePropertyId != null) {
      metricPropertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    } else {
      metricPropertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Component);
    }
    Map<String, PropertyInfo> amsMetrics = new HashMap<String, PropertyInfo>();
    if (metricPropertyIds.containsKey(HBASE_MASTER.name())) {
      amsMetrics.putAll(metricPropertyIds.get(HBASE_MASTER.name()));
    }
    if (metricPropertyIds.containsKey(HBASE_REGIONSERVER.name())) {
      amsMetrics.putAll(metricPropertyIds.get(HBASE_REGIONSERVER.name()));
    }
    if (!amsMetrics.isEmpty()) {
      super.getComponentMetrics().putAll(Collections.singletonMap(METRICS_COLLECTOR.name(), amsMetrics));
    }

    return super.getComponentMetrics();
  }

  /**
   * Return a set of @MetricsRequest object for each cluster.
   *
   * @param resources Set of resources asked to populate
   * @param request Original Request object used to check properties
   * @param ids Property ids to populate on the resource
   * @throws SystemException
   */
  private Map<String, Map<TemporalInfo, MetricsRequest>> getMetricsRequests(
              Set<Resource> resources, Request request, Set<String> ids) throws SystemException {

    Map<String, Map<TemporalInfo, MetricsRequest>> requestMap =
      new HashMap<String, Map<TemporalInfo, MetricsRequest>>();

    String collectorHostName = null;
    String collectorPort = null;

    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);
      // If a resource is not part of a cluster, do not return metrics since
      // we cannot decide which collector to reach
      if (StringUtils.isEmpty(clusterName)) {
        continue;
      }

      // Check liveliness of host
      if (!hostProvider.isCollectorHostLive(clusterName, TIMELINE_METRICS)) {
        if (printSkipPopulateMsgHostCounter.getAndIncrement() == 0) {
          LOG.info("METRICS_COLLECTOR host is not live. Skip populating " +
            "resources with metrics, next message will be logged after 1000 " +
            "attempts.");
        } else {
          printSkipPopulateMsgHostCounter.compareAndSet(1000, 0);
        }
        continue;
      }
      // reset
      printSkipPopulateMsgHostCounter.set(0);

      // Check liveliness of Collector
      if (!hostProvider.isCollectorComponentLive(clusterName, TIMELINE_METRICS)) {
        if (printSkipPopulateMsgHostCompCounter.getAndIncrement() == 0) {
          LOG.info("METRICS_COLLECTOR is not live. Skip populating resources " +
            "with metrics., next message will be logged after 1000 " +
            "attempts.");
        } else {
          printSkipPopulateMsgHostCompCounter.compareAndSet(1000, 0);
        }
        continue;
      }
      // reset
      printSkipPopulateMsgHostCompCounter.set(0);

      Map<TemporalInfo, MetricsRequest> requests = requestMap.get(clusterName);
      if (requests == null) {
        requests = new HashMap<TemporalInfo, MetricsRequest>();
        requestMap.put(clusterName, requests);
      }

      if (collectorHostName == null) {
        collectorHostName = hostProvider.getCollectorHostName(clusterName, TIMELINE_METRICS);
      }

      if (collectorPort == null) {
        collectorPort = hostProvider.getCollectorPort(clusterName, TIMELINE_METRICS);
      }

      for (String id : ids) {
        Map<String, PropertyInfo> propertyInfoMap = new HashMap<String, PropertyInfo>();

        String componentName = getOverridenComponentName(resource);

        Map<String, PropertyInfo> componentMetricMap = getComponentMetrics().get(componentName);

        // Not all components have metrics
        if (componentMetricMap != null && !componentMetricMap.containsKey(id)) {
          updateComponentMetricMap(componentMetricMap, id);
        }

        updatePropertyInfoMap(componentName, id, propertyInfoMap);

        for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {
          String propertyId = entry.getKey();
          PropertyInfo propertyInfo = entry.getValue();
          TemporalInfo temporalInfo = request.getTemporalInfo(id);

          if ((temporalInfo == null && propertyInfo.isPointInTime()) ||
            (temporalInfo != null && propertyInfo.isTemporal())) {

            MetricsRequest metricsRequest = requests.get(temporalInfo);
            if (metricsRequest == null) {
              metricsRequest = new MetricsRequest(temporalInfo,
                getAMSUriBuilder(collectorHostName,
                  collectorPort != null ? Integer.parseInt(collectorPort) : COLLECTOR_DEFAULT_PORT,
                  configuration.isHttpsEnabled()),
                  (String) resource.getPropertyValue(clusterNamePropertyId));
              requests.put(temporalInfo, metricsRequest);
            }
            metricsRequest.putResource(getComponentName(resource), resource);
            metricsRequest.putPropertyId(
              preprocessPropertyId(propertyInfo.getPropertyId(), getComponentName(resource)),
              propertyId);
            // If request is for a host metric we need to create multiple requests
            if (propertyInfo.isAmsHostMetric()) {
              metricsRequest.putHosComponentHostMetric(propertyInfo.getPropertyId());
            }
          }
        }
      }
    }

    return requestMap;
  }

  /**
   * Account for the processName added to the jvm metrics by the HadoopSink.
   * E.g.: jvm.RegionServer.JvmMetrics.GcTimeMillis
   *
   */
  private String preprocessPropertyId(String propertyId, String componentName) {
    if (propertyId.startsWith("jvm") && JVM_PROCESS_NAMES.keySet().contains(componentName)) {
      String newPropertyId = propertyId.replace("jvm.", "jvm." + JVM_PROCESS_NAMES.get(componentName));
      LOG.debug("Pre-process: " + propertyId + ", to: " + newPropertyId);
      return newPropertyId;
    }

    return propertyId;
  }

  static URIBuilder getAMSUriBuilder(String hostname, int port, boolean httpsEnabled) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(httpsEnabled ? "https" : "http");
    uriBuilder.setHost(hostname);
    uriBuilder.setPort(port);
    uriBuilder.setPath("/ws/v1/timeline/metrics");
    return uriBuilder;
  }
}
