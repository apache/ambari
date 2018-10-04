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
package org.apache.ambari.spi;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * The {@link ClusterInformation} class is used to pass the state of the cluster
 * as simple primitive values and collections. It contains the following types of information:
 * <ul>
 * <li>The name of a cluster
 * <li>The current desired configurations of a cluster
 * <li>The hosts where services and components are installed
 * <li>The security state of the cluster
 * </ul>
 */
public class ClusterInformation {

  /**
   * The cluster's current configurations.
   */
  private final Map<String, Map<String, String>> m_configurations;

  /**
   * The name of the cluster.
   */
  private final String m_clusterName;

  /**
   * {@code true} if the cluster is kerberized.
   */
  private final boolean m_isKerberized;

  /**
   * A simple representation of the cluster topology where the key is the
   * combination of service/component and the value is the set of hosts.
   */
  private final Map<String, Set<String>> m_topology;

  /**
   * Constructor.
   *
   * @param clusterName
   *          the name of the cluster.
   * @param isKerberized
   *          {@code true} if the cluster is Kerberized.
   * @param configurations
   *          a mapping of configuration type (such as foo-site) to the specific
   *          configurations (such as http.port : 8080).
   * @param topology
   *          a mapping of the cluster topology where the key is a combination
   *          of service / component and the value is the hosts where it is
   *          installed.
   */
  public ClusterInformation(String clusterName, boolean isKerberized,
      Map<String, Map<String, String>> configurations, Map<String, Set<String>> topology) {
    m_configurations = configurations;
    m_clusterName = clusterName;
    m_isKerberized = isKerberized;
    m_topology = topology;
  }

  /**
   * Gets the cluster name.
   *
   * @return the cluster name.
   */
  public String getClusterName() {
    return m_clusterName;
  }

  /**
   * Gets whether the cluster is Kerberized.
   *
   * @return {@code true} if the cluster is Kerberized.
   */
  public boolean isKerberized() {
    return m_isKerberized;
  }

  /**
   * Gets any hosts where the matching service and component are installed.
   *
   * @param serviceName
   *          the service name
   * @param componentName
   *          the component name
   * @return the set of hosts where the component is installed, or an empty set.
   */
  public Set<String> getHosts(String serviceName, String componentName) {
    Set<String> hosts = m_topology.get(serviceName + "/" + componentName);
    if (null == hosts) {
      hosts = Sets.newHashSet();
    }

    return hosts;
  }

  public Map<String, String> getConfigruationProperties(String configurationType) {
    Map<String, String> properties = m_configurations.get(configurationType);
    if (null == properties) {
      return Maps.newHashMap();
    }

    return properties;
  }

  /**
   * Gets a configuration value given the type and property name.
   *
   * @param configurationType
   *          the configuration type, such as foo-site.
   * @param propertyName
   *          the property name, such as http.port
   * @return the property value, or {@code null} if it does not exist.
   */
  public String getConfigurationProperty(String configurationType, String propertyName) {
    return getConfigruationProperties(configurationType).get(propertyName);
  }

}
