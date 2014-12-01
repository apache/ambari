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
import org.apache.ambari.server.controller.metrics.MetricsPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
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

import static org.apache.ambari.server.controller.metrics.MetricsPropertyProvider.MetricsService.TIMELINE_METRICS;
import static org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

public abstract class AMSPropertyProvider extends MetricsPropertyProvider {
  static final Map<String, String> TIMLINE_APPID_MAP = new HashMap<String, String>();
  private static ObjectMapper mapper;
  //private final HttpClient httpClient = new HttpClient();
  private final static ObjectReader timelineObjectReader;
  private static final DecimalFormat decimalFormat = new DecimalFormat("#.00");

  private static final Set<String> PERCENTAGE_METRIC;

  static {
    TIMLINE_APPID_MAP.put("HBASE_MASTER", "HBASE");
    TIMLINE_APPID_MAP.put("HBASE_REGIONSERVER", "HBASE");
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    //noinspection deprecation
    mapper.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
    timelineObjectReader = mapper.reader(TimelineMetrics.class);

    Set<String> temp = new HashSet<String>();
    temp.add("cpu_wio");
    temp.add("cpu_idle");
    temp.add("cpu_nice");
    temp.add("cpu_aidle");
    temp.add("cpu_system");
    temp.add("cpu_user");
    PERCENTAGE_METRIC = Collections.unmodifiableSet(temp);
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
   * The information required to make a single call to the Metrics service.
   */
  class MetricsRequest {
    private final TemporalInfo temporalInfo;
    private final Map<String, Set<Resource>> resources = new HashMap<String, Set<Resource>>();
    private final Map<String, Set<String>> metrics = new HashMap<String, Set<String>>();
    private final URIBuilder uriBuilder;
    private final String dummyHostName = "__SummaryInfo__";

    private MetricsRequest(TemporalInfo temporalInfo, URIBuilder uriBuilder) {
      this.temporalInfo = temporalInfo;
      this.uriBuilder = uriBuilder;
    }

    public void putResource(String hostname, Resource resource) {
      if (hostname == null) {
        hostname = dummyHostName;
      }
      Set<Resource> resourceSet = resources.get(hostname);
      if (resourceSet == null) {
        resourceSet = new HashSet<Resource>();
        resources.put(hostname, resourceSet);
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

    /**
     * Populate the associated resources by making a call to the Metrics
     * service.
     *
     * @return a collection of populated resources
     * @throws SystemException if unable to populate the resources
     */
    public Collection<Resource> populateResources() throws SystemException {
      // No open ended query support.
      if (temporalInfo == null || temporalInfo.getStartTime() == null ||
          temporalInfo.getEndTime() == null) {
        return Collections.emptySet();
      }

      for (Map.Entry<String, Set<Resource>> resourceEntry : resources.entrySet()) {
        String hostname = resourceEntry.getKey();
        Set<Resource> resourceSet = resourceEntry.getValue();

        for (Resource resource : resourceSet) {
          String metricsParam = getSetString(metrics.keySet(), -1);
          // Reuse uriBuilder
          uriBuilder.removeQuery();

          if (metricsParam.length() > 0) {
            uriBuilder.setParameter("metricNames", metricsParam);
          }

          if (hostname != null && !hostname.isEmpty() && !hostname.equals(dummyHostName)) {
            uriBuilder.setParameter("hostname", hostname);
          }

          String componentName = getComponentName(resource);
          if (componentName != null && !componentName.isEmpty()) {
            if (TIMLINE_APPID_MAP.containsKey(componentName)) {
              componentName = TIMLINE_APPID_MAP.get(componentName);
            }
            uriBuilder.setParameter("appId", componentName);
          }

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
                populateResource(resource, metric);
              }
            }

          } catch (IOException io) {
            LOG.warn("Error getting timeline metrics.", io);
          } finally {
            if (reader != null) {
              try {
                reader.close();
              } catch (IOException e) {
                if (LOG.isWarnEnabled()) {
                  LOG.warn("Unable to close http input steam : spec=" + spec, e);
                }
              }
            }
          }
        }
      }

      return Collections.emptySet();
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

  // Normalize percent values: Copied over from Ganglia Metric
  private static Number[][] getGangliaLikeDatapoints(TimelineMetric metric) {
    Number[][] datapointsArray = new Number[metric.getMetricValues().size()][2];
    int cnt = 0;

    for (Map.Entry<Long, Double> metricEntry : metric.getMetricValues().entrySet()) {
      Double value = metricEntry.getValue();
      Long time = metricEntry.getKey();
      if (time > 9999999999l) {
        time = time / 1000;
      }

      if (PERCENTAGE_METRIC.contains(metric.getMetricName())) {
        value = new Double(decimalFormat.format(value / 100));
      }

      datapointsArray[cnt][0] = value;
      datapointsArray[cnt][1] = time;
      cnt++;
    }

    return datapointsArray;
  }

  /**
   * Get value from the given metric.
   *
   * @param metric      the metric
   * @param isTemporal  indicates whether or not this a temporal metric
   *
   * @return a range of temporal data or a point in time value if not temporal
   */
  private static Object getValue(TimelineMetric metric, boolean isTemporal) {
    Number[][] dataPoints = getGangliaLikeDatapoints(metric);

    int length = dataPoints.length;
    if (isTemporal) {
      return length > 0 ? dataPoints : null;
    } else {
      // return the value of the last data point
      return length > 0 ? dataPoints[length - 1][0] : 0;
    }
  }

  protected static URIBuilder getUriBuilder(String hostname, int port) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme("http");
    uriBuilder.setHost(hostname);
    uriBuilder.setPort(port);
    uriBuilder.setPath("/ws/v1/timeline/metrics");
    return uriBuilder;
  }

  @Override
  public Set<Resource> populateResources(Set<Resource> resources,
              Request request, Predicate predicate) throws SystemException {

    Set<String> ids = getRequestPropertyIds(request, predicate);
    if (ids.isEmpty()) {
      return resources;
    }

    Map<String, Map<TemporalInfo, MetricsRequest>> requestMap =
      getMetricsRequests(resources, request, ids);

    // For each cluster
    for (Map.Entry<String, Map<TemporalInfo, MetricsRequest>> clusterEntry : requestMap.entrySet()) {
      // For each request
      for (MetricsRequest metricsRequest : clusterEntry.getValue().values() ) {
        metricsRequest.populateResources();
      }
    }

    return resources;
  }

  private Map<String, Map<TemporalInfo, MetricsRequest>> getMetricsRequests(
              Set<Resource> resources, Request request, Set<String> ids) throws SystemException {

    Map<String, Map<TemporalInfo, MetricsRequest>> requestMap =
      new HashMap<String, Map<TemporalInfo, MetricsRequest>>();

    String collectorHostName = null;
    String collectorPort = null;

    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);
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
        if (componentMetricMap != null &&
            !componentMetricMap.containsKey(id)) {
          updateComponentMetricMap(componentMetricMap, id);
        }

        getPropertyInfoMap(componentName, id, propertyInfoMap);

        for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {
          String propertyId = entry.getKey();
          PropertyInfo propertyInfo = entry.getValue();

          TemporalInfo temporalInfo = request.getTemporalInfo(id);

          if ((temporalInfo == null && propertyInfo.isPointInTime()) ||
            (temporalInfo != null && propertyInfo.isTemporal())) {

            MetricsRequest metricsRequest = requests.get(temporalInfo);
            if (metricsRequest == null) {
              metricsRequest = new MetricsRequest(temporalInfo,
                getUriBuilder(collectorHostName,
                  collectorPort != null ? Integer.parseInt(collectorPort) : 8188));
              requests.put(temporalInfo, metricsRequest);
            }
            metricsRequest.putResource(getHostName(resource), resource);
            metricsRequest.putPropertyId(propertyInfo.getPropertyId(), propertyId);
          }
        }
      }
    }

    return requestMap;
  }
}
