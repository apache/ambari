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

package org.apache.ambari.server.controller.ganglia;

import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Property provider implementation for a Ganglia source. This provider is specialized
 * to pull metrics from existing Ganglia reports.
 */
public class GangliaReportPropertyProvider implements PropertyProvider {

  /**
   * Set of property ids supported by this provider.
   */
  private final Set<String> propertyIds;

  private final Map<String, PropertyInfo> componentMetrics;

  private final StreamProvider streamProvider;

  private final GangliaHostProvider hostProvider;

  private final String clusterNamePropertyId;


  // ----- Constants --------------------------------------------------------

  protected final static Logger LOG =
      LoggerFactory.getLogger(GangliaReportPropertyProvider.class);

  private static final String GANGLIA_CLUSTER_NAME = "HDPSlaves";


  // ----- Constructors ------------------------------------------------------

  public GangliaReportPropertyProvider(Map<String, PropertyInfo> componentMetrics,
                                       StreamProvider streamProvider,
                                       GangliaHostProvider hostProvider,
                                       String clusterNamePropertyId) {

    this.componentMetrics      = componentMetrics;
    this.streamProvider        = streamProvider;
    this.hostProvider          = hostProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;

    propertyIds = new HashSet<String>(componentMetrics.keySet());
  }


  // ----- PropertyProvider --------------------------------------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate)
      throws SystemException {

    Set<Resource> keepers = new HashSet<Resource>();
    for (Resource resource : resources) {
      if (populateResource(resource, request, predicate)) {
        keepers.add(resource);
      }
    }
    return keepers;
  }

  @Override
  public Set<String> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    if (!this.propertyIds.containsAll(propertyIds)) {
      Set<String> unsupportedPropertyIds = new HashSet<String>(propertyIds);
      unsupportedPropertyIds.removeAll(this.propertyIds);
      return unsupportedPropertyIds;
    }
    return Collections.emptySet();
  }


  // ----- helper methods ----------------------------------------------------

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

    if (propertyIds.isEmpty()) {
      return true;
    }
    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    if (hostProvider.getGangliaCollectorHostName(clusterName) == null) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Attempting to get metrics but the Ganglia server is unknown. Resource=" + resource +
            " : Cluster=" + clusterName);
      }
      return true;
    }

    setProperties(resource, clusterName, request,
        PropertyHelper.getRequestPropertyIds(propertyIds, request, predicate));

    return true;
  }

  private boolean setProperties(Resource resource, String clusterName, Request request, Set<String> ids)
      throws SystemException {

    Map<String, Map<String, String>> propertyIdMaps = getPropertyIdMaps(request, ids);

    for (Map.Entry<String, Map<String, String>> entry : propertyIdMaps.entrySet()) {
      Map<String, String>  map = entry.getValue();
      String report = entry.getKey();

      String spec = getSpec(clusterName, report);

      try {
        List<GangliaMetric> gangliaMetrics = new ObjectMapper().readValue(streamProvider.readFrom(spec),
            new TypeReference<List<GangliaMetric>>() {});

        if (gangliaMetrics != null) {
          for (GangliaMetric gangliaMetric : gangliaMetrics) {

            String propertyId = map.get(gangliaMetric.getMetric_name());
            if (propertyId != null) {
              resource.setProperty(propertyId, getValue(gangliaMetric));
            }
          }
        }
      } catch (IOException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Caught exception getting Ganglia metrics : " + e + " : spec=" + spec);
        }
        return false;
      }
    }
    return true;
  }


  private Map<String, Map<String, String>> getPropertyIdMaps(Request request, Set<String> ids) {
    Map<String, Map<String, String>> propertyMap = new HashMap<String, Map<String, String>>();

    for (String propertyId : ids) {
      PropertyInfo propertyInfo = componentMetrics.get(propertyId);

      if (propertyInfo != null) {

        TemporalInfo temporalInfo = request.getTemporalInfo(propertyId);

        if (temporalInfo != null && propertyInfo.isTemporal()) {
          String propertyName = propertyInfo.getPropertyId();
          String report = null;
          // format : report_name.metric_name
          int dotIndex = propertyName.lastIndexOf('.');
          if (dotIndex != -1){
            report = propertyName.substring(0, dotIndex);
            propertyName = propertyName.substring(dotIndex + 1);
          }
          if (report !=  null) {
            Map<String, String> map = propertyMap.get(report);
            if (map == null) {
              map = new HashMap<String, String>();
              propertyMap.put(report, map);
            }
            map.put(propertyName, propertyId);
          }
        }
      }
    }
    return propertyMap;
  }

  /**
   * Get value from the given metric.
   *
   *
   * @param metric     the metric
   */
  private Object getValue(GangliaMetric metric) {
      return metric.getDatapoints();
  }

  /**
   * Get the spec to locate the Ganglia stream from the given
   * request info.
   *
   *
   * @param clusterName     the cluster name
   * @param report          the report
   *
   * @return the spec
   *
   * @throws SystemException if unable to ge the Ganglia Collector host name
   */
  protected String getSpec(String clusterName, String report) throws SystemException {

    StringBuilder sb = new StringBuilder();

    sb.append("http://").
        append(hostProvider.getGangliaCollectorHostName(clusterName)).
        append("/ganglia/graph.php?c=").
        append(GANGLIA_CLUSTER_NAME).
        append("&g=").
        append(report).
        append("&json=1");

    return sb.toString();
  }
}
