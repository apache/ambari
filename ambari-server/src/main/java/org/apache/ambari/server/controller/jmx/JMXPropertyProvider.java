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

package org.apache.ambari.server.controller.jmx;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Property provider implementation for JMX sources.
 */
public class JMXPropertyProvider implements PropertyProvider {

  protected static final PropertyId HOST_COMPONENT_HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("host_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("component_name", "HostRoles");

  private static final String CATEGORY_KEY = "tag.context";

  /**
   * Map of property ids supported by this provider.
   */
  private final Set<PropertyId> propertyIds;

  private final StreamProvider streamProvider;

  private final HostMappingProvider mappingProvider;

  private static final Map<String, String> JMX_PORTS = new HashMap<String, String>();

  static {
    JMX_PORTS.put("NAMENODE", "50070");
    JMX_PORTS.put("HBASE_MASTER", "60010");
    JMX_PORTS.put("JOBTRACKER", "50030");
    JMX_PORTS.put("DATANODE", "50075");
    JMX_PORTS.put("TASKTRACKER", "50060");
  }


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a JMX property provider.
   *
   * @param propertyIds     the property ids provided by this provider
   * @param streamProvider  the stream provider
   * @param mappingProvider the provider of host mapping information
   */
  public JMXPropertyProvider(Set<PropertyId> propertyIds,
                              StreamProvider streamProvider,
                              HostMappingProvider mappingProvider) {
    this.propertyIds = propertyIds;
    this.streamProvider = streamProvider;
    this.mappingProvider = mappingProvider;
  }


  // ----- PropertyProvider --------------------------------------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources,
                                         Request request,
                                         Predicate predicate)
      throws AmbariException {
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
   * Populate a resource by obtaining the requested JMX properties.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   *
   * @return true if the resource was successfully populated with the requested properties
   *
   * @throws AmbariException thrown if the resource cannot be populated
   */
  private boolean populateResource(Resource resource,
                                   Request request,
                                   Predicate predicate)
      throws AmbariException {

    if (getPropertyIds().isEmpty()) {
      return true;
    }

    Set<PropertyId> ids = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);

    Map<String, String> hosts = mappingProvider.getHostMap();

    String hostName = hosts.get(PropertyHelper.fixHostName(resource.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID)));
    String port = JMX_PORTS.get(resource.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID));

    if (hostName == null || port == null) {
      return true;
    }

    JMXMetrics metrics;

    String spec = getSpec(hostName + ":" + port);

    try {
      metrics = new ObjectMapper().readValue(streamProvider.readFrom(spec), JMXMetrics.class);
    } catch (IOException e) {
      throw new AmbariException("Can't get metrics : " + spec, e);
    }

    for (Map<String, String> propertyMap : metrics.getBeans()) {
      String category = propertyMap.get(CATEGORY_KEY);
      if (category != null) {
        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {

          PropertyId propertyId = PropertyHelper.getPropertyId(entry.getKey(), category);

          if (ids.contains(propertyId)) {
            resource.setProperty(propertyId, entry.getValue());
          }
        }
      }
    }
    return true;
  }

  /**
   * Get the spec to locate the JMX stream from the given source
   *
   * @param jmxSource  the source (host and port)
   *
   * @return the spec
   */
  protected static String getSpec(String jmxSource) {
    return "http://" + jmxSource + "/jmx?qry=Hadoop:*";
  }
}
