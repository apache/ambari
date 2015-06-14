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

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.MetricsPropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.ambari.server.Role.HBASE_MASTER;
import static org.apache.ambari.server.Role.HBASE_REGIONSERVER;
import static org.apache.ambari.server.Role.METRICS_COLLECTOR;
import static org.apache.ambari.server.controller.metrics.MetricsPaddingMethod.ZERO_PADDING_PARAM;
import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.TIMELINE_METRICS;
import static org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

public abstract class AMSPropertyProvider extends MetricsPropertyProvider {
  private static ObjectMapper mapper;
  private final static ObjectReader timelineObjectReader;
  private static final String METRIC_REGEXP_PATTERN = "\\([^)]*\\)";
  private static final int COLLECTOR_DEFAULT_PORT = 6188;

  static {
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    //noinspection deprecation
    mapper.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
    timelineObjectReader = mapper.reader(TimelineMetrics.class);
  }

  public AMSPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                             StreamProvider streamProvider,
                             ComponentSSLConfiguration configuration,
                             MetricHostProvider hostProvider,
                             String clusterNamePropertyId,
                             String hostNamePropertyId,
                             String componentNamePropertyId) {

    super(componentPropertyInfoMap, streamProvider, configuration,
      hostProvider, clusterNamePropertyId, hostNamePropertyId,
      componentNamePropertyId);
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

    private TimelineMetrics getTimelineMetricsForSpec(String spec) {
      TimelineMetrics timelineMetrics = null;

      LOG.debug("Metrics request url = " + spec);
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(streamProvider.readFrom(spec)));
        timelineMetrics = timelineObjectReader.readValue(reader);
        LOG.debug("Timeline metrics response => " + timelineMetrics);

      } catch (IOException io) {
        String errorMsg = "Error getting timeline metrics.";
        if (LOG.isDebugEnabled()) {
          LOG.error(errorMsg, io);
        } else {
          if (io instanceof SocketTimeoutException) {
            errorMsg += " Can not connect to collector, socket error.";
          }
          LOG.error(errorMsg);
        }
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
              if (LOG.isDebugEnabled()) {
                LOG.warn("Unable to close http input stream : spec=" + spec, e);
              } else {
                LOG.warn("Unable to close http input stream : spec=" + spec);
              }
            }
          }
        }
      }

      return timelineMetrics;
    }

    /**
     * Populate the associated resources by making a call to the Metrics
     * service.
     *
     * @return a collection of populated resources
     * @throws SystemException if unable to populate the resources
     */
    public Collection<Resource> populateResources() throws SystemException {
      // No open ended query support.
      if (temporalInfo != null && (temporalInfo.getStartTime() == null
          || temporalInfo.getEndTime() == null)) {
        return Collections.emptySet();
      }

      for (Map.Entry<String, Set<Resource>> resourceEntry : resources.entrySet()) {
        String componentName = resourceEntry.getKey();
        Set<Resource> resourceSet = resourceEntry.getValue();

          TimelineMetrics timelineMetrics;
          // Allow for multiple requests since host metrics for a
          // hostcomponent need the HOST appId
          if (hostComponentHostMetrics.isEmpty()) { //HOST
            String spec = getSpec(componentName);
            timelineMetrics = getTimelineMetricsForSpec(spec);
          } else {
            Set<String> specs = getSpecsForHostComponentMetrics(componentName);
            timelineMetrics = new TimelineMetrics();
            for (String spec : specs) {
              if (!StringUtils.isEmpty(spec)) {
                TimelineMetrics metrics = getTimelineMetricsForSpec(spec);
                if (metrics != null) {
                  timelineMetrics.getMetrics().addAll(metrics.getMetrics());
                }
              }
            }
          }
        Map<String, Set<TimelineMetric>> metricsMap = new HashMap<String, Set<TimelineMetric>>();
        Set<String> patterns = createPatterns(metrics.keySet());
        if (timelineMetrics != null) {
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
                // Pad zeros or nulls if needed
                metricsPaddingMethod.applyPaddingStrategy(metric, temporalInfo);
                populateResource(resource, metric);
              }
            }
          }
        }
      }

      return Collections.emptySet();
    }

    /**
     * Return separate specs for : host component metrics and host component
     * host metrics.
     * @return @Set Urls
     */
    private Set<String> getSpecsForHostComponentMetrics(String componentName) {
      Set<String> nonHostComponentMetrics = new HashSet<String>(metrics.keySet());
      nonHostComponentMetrics.removeAll(hostComponentHostMetrics);

      Set<String> specs = new HashSet<String>();
      String hostnames = getHostnames(resources.get(componentName));
      if (!hostComponentHostMetrics.isEmpty()) {
        String hostComponentHostMetricParams = getSetString(processRegexps(hostComponentHostMetrics), -1);
        setQueryParams(hostComponentHostMetricParams, hostnames, true, componentName);
        specs.add(uriBuilder.toString());
      }

      if (!nonHostComponentMetrics.isEmpty()) {
        String nonHostComponentHostMetricParams = getSetString(processRegexps(nonHostComponentMetrics), -1);
        setQueryParams(nonHostComponentHostMetricParams, hostnames, false, componentName);
        specs.add(uriBuilder.toString());
      }
      return specs;
    }

    private void setQueryParams(String metricsParam,
                                String hostname, boolean isHostMetric, String componentName) {
      // Reuse uriBuilder
      uriBuilder.removeQuery();

      if (metricsParam.length() > 0) {
        uriBuilder.setParameter("metricNames", metricsParam);
      }

      if (hostname != null && !hostname.isEmpty()) {
        uriBuilder.setParameter("hostname", hostname);
      }

      if (isHostMetric) {
        uriBuilder.setParameter("appId", "HOST");
      } else {
        if (componentName != null && !componentName.isEmpty()
          && !componentName.equalsIgnoreCase("HOST")) {
          StackId stackId;
          try {
            AmbariManagementController managementController = AmbariServer.getController();
            stackId = managementController.getClusters().getCluster(clusterName).getCurrentStackVersion();
            if (stackId != null) {
              String stackName = stackId.getStackName();
              String version = stackId.getStackVersion();
              AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();
              String serviceName = ambariMetaInfo.getComponentToService(stackName,version,componentName);
              String timeLineAppId = ambariMetaInfo.getComponent(stackName, version, serviceName, componentName).getTimelineAppid();
              if (timeLineAppId != null){
                componentName = timeLineAppId;
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
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

    private String getSpec(String componentName) {
      String metricsParam = getSetString(processRegexps(metrics.keySet()), -1);
      String hostnames = getHostnames(resources.get(componentName));
      setQueryParams(metricsParam, hostnames, false, componentName);

      return uriBuilder.toString();
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

    private void populateResource(Resource resource, TimelineMetric metric) {
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
              if (metricsMap.containsKey(propertyId)){
                if (containsArguments(propertyId)) {
                  int i = 1;
                  for (String param : parameterList) {
                    propertyId = substituteArgument(propertyId, "$" + i, param);
                    ++i;
                  }
                }
                Object value = getValue(metric, temporalInfo != null);
                if (value != null) {
                  resource.setProperty(propertyId, value);
                }
              }
            }
          }
        }
      }
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
        LOG.info("METRICS_COLLECTOR host is not live. Skip populating " +
            "resources with metrics.");
        continue;
      }

      // Check liveliness of Collector
      if (!hostProvider.isCollectorComponentLive(clusterName, TIMELINE_METRICS)) {
        LOG.info("METRICS_COLLECTOR is not live. Skip populating resources" +
            " with metrics.");
        continue;
      }

      Map<TemporalInfo, MetricsRequest> requests = requestMap.get(clusterName);
      if (requests == null) {
        requests = new HashMap<TemporalInfo, MetricsRequest>();
        requestMap.put(clusterName, requests);
      }

      if (collectorHostName == null) {
        collectorHostName = hostProvider.getCollectorHostName(clusterName, TIMELINE_METRICS);
      }

      if (collectorPort == null) {
        collectorPort = hostProvider.getCollectorPortName(clusterName, TIMELINE_METRICS);
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
                  collectorPort != null ? Integer.parseInt(collectorPort) : COLLECTOR_DEFAULT_PORT),
                 (String) resource.getPropertyValue(clusterNamePropertyId));
              requests.put(temporalInfo, metricsRequest);
            }
            metricsRequest.putResource(getComponentName(resource), resource);
            metricsRequest.putPropertyId(propertyInfo.getPropertyId(), propertyId);
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

  static URIBuilder getAMSUriBuilder(String hostname, int port) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme("http");
    uriBuilder.setHost(hostname);
    uriBuilder.setPort(port);
    uriBuilder.setPath("/ws/v1/timeline/metrics");
    return uriBuilder;
  }
}
