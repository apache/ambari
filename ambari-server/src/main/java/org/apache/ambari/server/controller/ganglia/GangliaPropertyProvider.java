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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Property provider implementation for a Ganglia source.
 */
public class GangliaPropertyProvider implements PropertyProvider {

  /**
   * Set of property ids supported by this provider.
   */
  private final Set<PropertyId> propertyIds;

  private final Map<String, Map<PropertyId, String>> componentMetrics;

  private final StreamProvider streamProvider;

  private final GangliaHostProvider hostProvider;

  private final PropertyId clusterNamePropertyId;

  private final PropertyId hostNamePropertyId;

  private final PropertyId componentNamePropertyId;


  /**
   * Map of Ganglia cluster names keyed by component type.
   */
  public static final Map<String, String> GANGLIA_CLUSTER_NAMES = new HashMap<String, String>();

  static {
    GANGLIA_CLUSTER_NAMES.put("NAMENODE",           "HDPNameNode");
    GANGLIA_CLUSTER_NAMES.put("DATANODE",           "HDPSlaves");
    GANGLIA_CLUSTER_NAMES.put("JOBTRACKER",         "HDPJobTracker");
    GANGLIA_CLUSTER_NAMES.put("TASKTRACKER",        "HDPSlaves");
    GANGLIA_CLUSTER_NAMES.put("HBASE_MASTER",       "HDPHBaseMaster");
    GANGLIA_CLUSTER_NAMES.put("HBASE_CLIENT",       "HDPSlaves");
    GANGLIA_CLUSTER_NAMES.put("HBASE_REGIONSERVER", "HDPSlaves");
  }


  // ----- Constructors ------------------------------------------------------

  public GangliaPropertyProvider(Map<String, Map<PropertyId, String>> componentMetrics,
                                 StreamProvider streamProvider,
                                 GangliaHostProvider hostProvider,
                                 PropertyId clusterNamePropertyId,
                                 PropertyId hostNamePropertyId,
                                 PropertyId componentNamePropertyId) {
    this.componentMetrics         = componentMetrics;
    this.streamProvider           = streamProvider;
    this.hostProvider             = hostProvider;
    this.clusterNamePropertyId    = clusterNamePropertyId;
    this.hostNamePropertyId       = hostNamePropertyId;
    this.componentNamePropertyId  = componentNamePropertyId;

    propertyIds = new HashSet<PropertyId>();
    for (Map.Entry<String, Map<PropertyId, String>> entry : componentMetrics.entrySet()) {
      propertyIds.addAll(entry.getValue().keySet());
    }
  }


  // ----- PropertyProvider --------------------------------------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) throws AmbariException{
    Set<Resource> keepers = new HashSet<Resource>();
    for (Resource resource : resources) {
      if (populateResource(resource, request, predicate)) {
        keepers.add(resource);
      }
    }
    return keepers;
  }

  @Override
  public Set<PropertyId> getPropertyIds() {
    return propertyIds;
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
   * @throws AmbariException thrown if the resource cannot be populated
   */
  private boolean populateResource(Resource resource, Request request, Predicate predicate) throws AmbariException{

    if (getPropertyIds().isEmpty()) {
      return true;
    }
    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    // TODO : what should we do if there is no Ganglia server?
    if (hostProvider.getGangliaHostClusterMap(clusterName) == null ||
        hostProvider.getGangliaCollectorHostName(clusterName) == null) {
      return true;
    }

    String hostName = hostNamePropertyId == null ?
        null : (String) resource.getPropertyValue(hostNamePropertyId);

    String componentName;
    String gangliaClusterName;
    if (componentNamePropertyId == null) {
      componentName = "*";
      gangliaClusterName = hostProvider.getGangliaHostClusterMap(clusterName).get(hostName);
    } else {
      componentName = (String) resource.getPropertyValue(componentNamePropertyId);
      gangliaClusterName = GANGLIA_CLUSTER_NAMES.get(componentName);
    }

    Map<PropertyId, String> metrics = componentMetrics.get(componentName);

    if (metrics == null || gangliaClusterName == null) {
      return true;
    }

    Set<PropertyId> ids = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);

    for (PropertyId propertyId : ids) {

      String metricName = metrics.get(propertyId);

      if (metricName != null) {
        boolean temporal = propertyId.isTemporal();

        String spec = getSpec(clusterName, gangliaClusterName,
            hostName == null ? null : PropertyHelper.fixHostName(hostName), metricName,
            temporal ? request.getTemporalInfo(propertyId) : null);

        try {
          List<GangliaMetric> properties = new ObjectMapper().readValue(streamProvider.readFrom(spec),
              new TypeReference<List<GangliaMetric>>() {});

          if (properties != null) {
            resource.setProperty(propertyId, getValue(properties.get(0), temporal));
          }
        } catch (IOException e) {
          // TODO : log this
//          throw new AmbariException("Can't get metric : " + metricName, e);
        }
      }
    }
    return true;
  }

  /**
   * Get value from the given metric.
   *
   * @param metric     the metric
   * @param isTemporal indicates whether or not this a temporal metric
   *
   * @return the string representation of the temporal data
   */
  private Object getValue(GangliaMetric metric, boolean isTemporal) {
    double[][] dataPoints = metric.getDatapoints();

    if (!isTemporal) {
      int valuePosition = dataPoints.length - 1;
      // discard last data point ... seems to always be zero
      if (valuePosition > 0) {
        valuePosition--;
      }
      return dataPoints[valuePosition][0];
    }

    boolean first = true;
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[");
    for (double[] m : dataPoints) {
      if (!first) {
        stringBuilder.append(",");
      }
      stringBuilder.append("[");
      stringBuilder.append(m[0]);
      stringBuilder.append(",");
      stringBuilder.append((long) m[1]);
      stringBuilder.append("]");
      first = false;
    }
    stringBuilder.append("]");
    return stringBuilder.toString();
  }

  /**
   * Get the spec to locate the Ganglia stream from the given
   * request info.
   *
   * @param clusterName     the cluster name
   * @param gangliaCluster  the ganglia cluster name
   * @param host            the host name
   * @param metric          the metric
   * @param temporalInfo    the temporal data; may be null
   *
   * @return the spec
   */
  protected String getSpec(String clusterName, String gangliaCluster,
                                  String host,
                                  String metric,
                                  TemporalInfo temporalInfo) {

    StringBuilder sb = new StringBuilder();

    sb.append("http://").
        append(hostProvider.getGangliaCollectorHostName(clusterName)).
        append("/ganglia/graph.php?c=").
        append(gangliaCluster);

    if(host != null) {
      sb.append("&h=").append(host);
    }

    sb.append("&m=").append(metric);

    if (temporalInfo == null) {
      sb.append("&r=day");
    } else {
      long startTime = temporalInfo.getStartTime();
      if (startTime != -1) {
        sb.append("&cs=").append(startTime);
      }

      long endTime = temporalInfo.getEndTime();
      if (endTime != -1) {
        sb.append("&ce=").append(endTime);
      }

      long step = temporalInfo.getStep();
      if (step != -1) {
        sb.append("&step=").append(step);
      }
    }

    sb.append("&json=1");

    return sb.toString();
  }
}
