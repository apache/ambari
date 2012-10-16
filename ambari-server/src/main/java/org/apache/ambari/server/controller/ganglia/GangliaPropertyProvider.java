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
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
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

  protected static final PropertyId HOST_COMPONENT_HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("host_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("component_name", "HostRoles");


  /**
   * Set of property ids supported by this provider.
   */
  private final Set<PropertyId> propertyIds;

  private final StreamProvider streamProvider;

  private final String gangliaCollectorHostName;

  /**
   * Map of Ganglia cluster names keyed by component type.
   */
  private static final Map<String, String> GANGLIA_CLUSTER_NAMES = new HashMap<String, String>();

  static {
    GANGLIA_CLUSTER_NAMES.put("NAMENODE",    "HDPNameNode");
    GANGLIA_CLUSTER_NAMES.put("JOBTRACKER",  "HDPJobTracker");
    GANGLIA_CLUSTER_NAMES.put("DATANODE",    "HDPSlaves");
    GANGLIA_CLUSTER_NAMES.put("TASKTRACKER", "HDPSlaves");
  }


  // ----- Constructors ------------------------------------------------------

  public GangliaPropertyProvider(Set<PropertyId> propertyIds,
                                 StreamProvider streamProvider,
                                 String gangliaCollectorHostName) {
    this.propertyIds              = propertyIds;
    this.streamProvider           = streamProvider;
    this.gangliaCollectorHostName = gangliaCollectorHostName;
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
   * Populate a resource by obtaining the requested Ganglia metrics.
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
    Set<PropertyId> ids = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);

    String hostName           = PropertyHelper.fixHostName((String) resource.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID));
    String componentName      = (String) resource.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
    String gangliaClusterName = GANGLIA_CLUSTER_NAMES.get(componentName);

    if (gangliaClusterName == null) {
      return true;
    }

    for (PropertyId propertyId : ids) {

// TODO : ignoring category for now..
//      String category = propertyId.getCategory();
//      String property = (category == null || category.length() == 0 ? "" : category + ".") +
//          propertyId.getName();
      String property = propertyId.getName();

      Request.TemporalInfo temporalInfo = request.getTemporalInfo(propertyId);
      String spec = getSpec(gangliaClusterName, hostName, property,
          temporalInfo.getStartTime(), temporalInfo.getEndTime(), temporalInfo.getStep());

      try {
        List<GangliaMetric> properties = new ObjectMapper().readValue(streamProvider.readFrom(spec),
            new TypeReference<List<GangliaMetric>>() {
        });
        resource.setProperty(propertyId, getTemporalValue(properties.get(0)));
      } catch (IOException e) {
        throw new AmbariException("Can't get metrics : " + property, e);
      }
    }
    return true;
  }

  /**
   * Get a string representation of the temporal data from the given metric.
   *
   * @param metric  the metric
   *
   * @return the string representation of the temporal data
   */
  private String getTemporalValue(GangliaMetric metric) {
    double[][] dataPoints = metric.getDatapoints();

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
   * @param gangliaCluster  the ganglia cluster name
   * @param host            the host name
   * @param metric          the metric
   * @param startTime       the start time of the temporal data
   * @param endTime         the end time of the temporal data
   * @param step            the step for the temporal data
   *
   * @return the spec
   */
  protected String getSpec(String gangliaCluster,
                                  String host,
                                  String metric,
                                  Long startTime,
                                  Long endTime,
                                  Long step) {

    StringBuilder sb = new StringBuilder();

    sb.append("http://").
       append(gangliaCollectorHostName).
       append("/ganglia/graph.php?c=").
       append(gangliaCluster).
       append("&h=").
       append(host == null ? "" : host).
       append("&m=").
       append(metric);

    if (startTime != null) {
      sb.append("&cs=").append(startTime);
    }
    if (endTime != null) {
      sb.append("&ce=").append(endTime);
    }
    if (step != null) {
      sb.append("&step=").append(step);
    }
    sb.append("&json=1");

    return sb.toString();
  }
}

