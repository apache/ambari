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

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.MetricsPaddingMethod;
import org.apache.ambari.server.controller.metrics.MetricsPropertyProvider;
import org.apache.ambari.server.controller.metrics.MetricsReportPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.ambari.server.controller.metrics.MetricsPaddingMethod.ZERO_PADDING_PARAM;
import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.TIMELINE_METRICS;
import static org.apache.ambari.server.controller.utilities.PropertyHelper.updateMetricsWithAggregateFunctionSupport;

public class AMSReportPropertyProvider extends MetricsReportPropertyProvider {
  private static ObjectMapper mapper;
  private final static ObjectReader timelineObjectReader;
  private MetricsPaddingMethod metricsPaddingMethod;

  static {
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    //noinspection deprecation
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    timelineObjectReader = mapper.reader(TimelineMetrics.class);
  }

  public AMSReportPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                                 StreamProvider streamProvider,
                                 ComponentSSLConfiguration configuration,
                                 MetricHostProvider hostProvider,
                                 String clusterNamePropertyId) {

    super(componentPropertyInfoMap, streamProvider, configuration,
      hostProvider, clusterNamePropertyId);
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

  @Override
  public Set<Resource> populateResources(Set<Resource> resources,
               Request request, Predicate predicate) throws SystemException {

    Set<Resource> keepers = new HashSet<Resource>();
    for (Resource resource : resources) {
      if (populateResource(resource, request, predicate)) {
        keepers.add(resource);
      }
    }
    return keepers;
  }

  /**
   * Populate a resource by obtaining the requested Ganglia RESOURCE_METRICS.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   *
   * @return true if the resource was successfully populated with the requested properties
   *
   * @throws SystemException if unable to populate the resource
   */
  private boolean populateResource(Resource resource, Request request, Predicate predicate)
      throws SystemException {

    Set<String> propertyIds = getPropertyIds();

    if (propertyIds.isEmpty()) {
      return true;
    }

    metricsPaddingMethod = DEFAULT_PADDING_METHOD;

    Set<String> requestPropertyIds = request.getPropertyIds();
    if (requestPropertyIds != null && !requestPropertyIds.isEmpty()) {
      for (String propertyId : requestPropertyIds) {
        if (propertyId.startsWith(ZERO_PADDING_PARAM)) {
          String paddingStrategyStr = propertyId.substring(ZERO_PADDING_PARAM.length() + 1);
          metricsPaddingMethod = new MetricsPaddingMethod(
            MetricsPaddingMethod.PADDING_STRATEGY.valueOf(paddingStrategyStr));
        }
      }
    }

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    // Check liveliness of host
    if (!hostProvider.isCollectorHostLive(clusterName, TIMELINE_METRICS)) {
      LOG.info("METRICS_COLLECTOR host is not live. Skip populating " +
        "resources with metrics.");
      return true;
    }

    // Check liveliness of Collector
    if (!hostProvider.isCollectorComponentLive(clusterName, TIMELINE_METRICS)) {
      LOG.info("METRICS_COLLECTOR is not live. Skip populating resources" +
        " with metrics.");
      return true;
    }

    setProperties(resource, clusterName, request, getRequestPropertyIds(request, predicate));

    return true;
  }

  private void setProperties(Resource resource, String clusterName,
                            Request request, Set<String> ids) throws SystemException {

    Map<String, MetricReportRequest> reportRequestMap = getPropertyIdMaps(request, ids);
    String host = hostProvider.getCollectorHostName(clusterName, TIMELINE_METRICS);
    String port = hostProvider.getCollectorPortName(clusterName, TIMELINE_METRICS);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder(host,
      port != null ? Integer.parseInt(port) : 8188);

    for (Map.Entry<String, MetricReportRequest> entry : reportRequestMap.entrySet()) {
      MetricReportRequest reportRequest = entry.getValue();
      TemporalInfo temporalInfo = reportRequest.getTemporalInfo();
      Map<String, String> propertyIdMap = reportRequest.getPropertyIdMap();

      uriBuilder.removeQuery();
      // Call with hostname = null
      uriBuilder.addParameter("metricNames",
        MetricsPropertyProvider.getSetString(propertyIdMap.keySet(), -1));

      uriBuilder.setParameter("appId", "HOST");
      long startTime = temporalInfo.getStartTime();
      if (startTime != -1) {
        uriBuilder.setParameter("startTime", String.valueOf(startTime));
      }

      long endTime = temporalInfo.getEndTime();
      if (endTime != -1) {
        uriBuilder.setParameter("endTime", String.valueOf(endTime));
      }

      BufferedReader reader = null;
      String spec = uriBuilder.toString();
      try {
        LOG.debug("Metrics request url =" + spec);
        reader = new BufferedReader(new InputStreamReader(streamProvider.readFrom(spec)));

        TimelineMetrics timelineMetrics = timelineObjectReader.readValue(reader);
        LOG.debug("Timeline metrics response => " + timelineMetrics);

        for (TimelineMetric metric : timelineMetrics.getMetrics()) {
          if (metric.getMetricName() != null && metric.getMetricValues() != null) {
            // Pad zeros or nulls if needed
            metricsPaddingMethod.applyPaddingStrategy(metric, temporalInfo);

            String propertyId = propertyIdMap.get(metric.getMetricName());
            if (propertyId != null) {
              resource.setProperty(propertyId, getValue(metric, true));
            }
          }
        }

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
                LOG.warn("Unable to close http input steam : spec=" + spec, e);
              } else {
                LOG.warn("Unable to close http input steam : spec=" + spec);
              }
            }
          }
        }
      }
    }
  }

  private Map<String, MetricReportRequest> getPropertyIdMaps(Request request, Set<String> ids) {
    Map<String, MetricReportRequest> propertyMap = new HashMap<String, MetricReportRequest>();

    for (String id : ids) {
      Map<String, PropertyInfo> propertyInfoMap = getPropertyInfoMap("*", id);

      for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {
        PropertyInfo propertyInfo = entry.getValue();
        String propertyId = entry.getKey();
        String amsId = propertyInfo.getAmsId();

        TemporalInfo temporalInfo = request.getTemporalInfo(id);

        if (temporalInfo != null && propertyInfo.isTemporal()) {
          String propertyName = propertyInfo.getPropertyId();
          String report = null;
          // format : report_name.metric_name
          int dotIndex = propertyName.lastIndexOf('.');
          if (dotIndex != -1){
            report = propertyName.substring(0, dotIndex);
          }
          if (report !=  null) {
            MetricReportRequest reportRequest = propertyMap.get(report);
            if (reportRequest == null) {
              reportRequest = new MetricReportRequest();
              propertyMap.put(report, reportRequest);
              reportRequest.setTemporalInfo(temporalInfo);
            }
            reportRequest.addPropertyId(amsId, propertyId);
          }
        }
      }
    }
    return propertyMap;
  }

  class MetricReportRequest {
    private TemporalInfo temporalInfo;
    private Map<String, String> propertyIdMap = new HashMap<String, String>();

    public TemporalInfo getTemporalInfo() {
      return temporalInfo;
    }

    public void setTemporalInfo(TemporalInfo temporalInfo) {
      this.temporalInfo = temporalInfo;
    }

    public Map<String, String> getPropertyIdMap() {
      return propertyIdMap;
    }

    public void addPropertyId(String propertyName, String propertyId) {
      propertyIdMap.put(propertyName, propertyId);
    }
  }
}
