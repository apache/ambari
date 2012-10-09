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

import org.apache.ambari.server.controller.internal.PropertyIdImpl;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Property provider implementation for JMX sources.
 */
public class JMXPropertyProvider implements PropertyProvider {

  /**
   * Map of property ids supported by this provider.
   */
  private final Set<PropertyId> propertyIds;

  private final Map<String, String> hosts;

  private static final Map<String, String> JMX_PORTS = new HashMap<String, String>();

  static {
    JMX_PORTS.put("NAMENODE", "50070");
    JMX_PORTS.put("HBASE_MASTER", "60010");
    JMX_PORTS.put("JOBTRACKER", "50030");
    JMX_PORTS.put("DATANODE", "50075");
    JMX_PORTS.put("TASKTRACKER", "50060");
  }

  private JMXPropertyProvider(Resource.Type type, Map<String, String> hosts) {
    this.hosts = hosts;
    this.propertyIds = PropertyHelper.getPropertyIds(type, "JMX");
  }

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) {
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


  private boolean populateResource(Resource resource, Request request, Predicate predicate) {

    if (getPropertyIds().isEmpty()) {
      return true;
    }

    Set<PropertyId> ids = new HashSet<PropertyId>(request.getPropertyIds());
    if (ids == null || ids.isEmpty()) {
      ids = getPropertyIds();
    } else {
      if (predicate != null) {
        ids.addAll(PredicateHelper.getPropertyIds(predicate));
      }
      ids.retainAll(getPropertyIds());
    }

    String hostName = hosts.get(resource.getPropertyValue(new PropertyIdImpl("host_name", "HostRoles", false)));
    String port = JMX_PORTS.get(resource.getPropertyValue(new PropertyIdImpl("component_name", "HostRoles", false)));

    String jmxSource = hostName + ":" + port;

    if (hostName == null || port == null || jmxSource == null) {
      return true;
    }

    JMXMetrics metrics = JMXHelper.getJMXMetrics(jmxSource, null);

    for (Map<String, String> propertyMap : metrics.getBeans()) {
      String category = propertyMap.get("tag.context");
      if (category != null) {
        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
          String name = entry.getKey();

          PropertyIdImpl propertyId = new PropertyIdImpl(name, category, false);

          if (ids.contains(propertyId)) {
            resource.setProperty(propertyId, entry.getValue());
          }
        }
      }
    }
    return true;
  }

  /**
   * Factory method.
   *
   * @param type the {@link Resource.Type resource type}
   * @return a new {@link PropertyProvider} instance
   */
  public static PropertyProvider create(Resource.Type type, Map<String, String> hosts) {
    return new JMXPropertyProvider(type, hosts);
  }
}
