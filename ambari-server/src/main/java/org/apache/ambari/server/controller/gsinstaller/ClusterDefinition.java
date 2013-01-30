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

package org.apache.ambari.server.controller.gsinstaller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines the cluster created by gsInstaller.
 */
public class ClusterDefinition {

  private static final String CLUSTER_DEFINITION_FILE = "gsInstaller-hosts.txt";
  private static final String DEFAULT_CLUSTER_NAME    = "ambari";
  private static final String CLUSTER_NAME_TAG        = "CLUSTER=";
  private static final String DEFAULT_VERSION_ID      = "HDP-1.2.0";
  private static final String VERSION_ID_TAG          = "VERSION=";

  private final Set<String> services = new HashSet<String>();
  private final Set<String> hosts = new HashSet<String>();
  private final Map<String, Set<String>> components = new HashMap<String, Set<String>>();
  private final Map<String, Map<String, Set<String>>> hostComponents = new HashMap<String, Map<String, Set<String>>>();

  private String clusterName;
  private String versionId;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a cluster definition.
   */
  public ClusterDefinition() {
    clusterName = DEFAULT_CLUSTER_NAME;
    versionId   = DEFAULT_VERSION_ID;
    readClusterDefinition();
  }


  // ----- ClusterDefinition -------------------------------------------------

  /**
   * Get the name of the cluster.
   *
   * @return the cluster name
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Get the name of the cluster.
   *
   * @return the cluster name
   */
  public String getVersionId() {
    return versionId;
  }

  /**
   * Get the services for the cluster.
   *
   * @return the set of service names
   */
  public Set<String> getServices() {
    return services;
  }

  /**
   * Get the hosts for the cluster.
   *
   * @return the set of hosts names
   */
  public Set<String> getHosts() {
    return hosts;
  }

  /**
   * Get the components for the given service.
   *
   * @param service  the service name
   *
   * @return the set of component names for the given service name
   */
  public Set<String> getComponents(String service) {
    return components.get(service);
  }

  /**
   * Get the host components for the given service and host.
   *
   * @param service  the service name
   * @param host     the host name
   *
   * @return the set of host component names for the given service and host names
   */
  public Set<String> getHostComponents(String service, String host) {
    Set<String> resultSet = null;
    Map<String, Set<String>> serviceHostComponents = hostComponents.get(service);
    if (serviceHostComponents != null) {
      resultSet = serviceHostComponents.get(host);
    }
    return resultSet == null ? Collections.<String>emptySet() : resultSet;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Read the gsInstaller cluster definition file.
   */
  private void readClusterDefinition() {
    try {
      InputStream    is = this.getClass().getClassLoader().getResourceAsStream(CLUSTER_DEFINITION_FILE);
      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.startsWith(CLUSTER_NAME_TAG)) {
          clusterName = line.substring(CLUSTER_NAME_TAG.length());
        }
        else if (line.startsWith(VERSION_ID_TAG)) {
          versionId = line.substring(VERSION_ID_TAG.length());
        }
        else {
          String[] parts = line.split("\\s+");
          assert(parts.length == 3);

          String serviceName   = parts[0];
          String componentName = parts[1];
          String hostName      = parts[2];

          services.add(serviceName);
          Set<String> serviceComponents = components.get(serviceName);
          if (serviceComponents == null) {
            serviceComponents = new HashSet<String>();
            components.put(serviceName, serviceComponents);
          }
          serviceComponents.add(componentName);

          Map<String, Set<String>> serviceHostComponents = hostComponents.get(serviceName);
          if (serviceHostComponents == null) {
            serviceHostComponents = new HashMap<String, Set<String>>();
            hostComponents.put(serviceName, serviceHostComponents);
          }

          Set<String> hostHostComponents = serviceHostComponents.get(hostName);
          if (hostHostComponents == null) {
            hostHostComponents = new HashSet<String>();
            serviceHostComponents.put(hostName, hostHostComponents);
          }
          hostHostComponents.add(componentName);
          hosts.add(hostName);
        }
      }
    } catch (IOException e) {
      String msg = "Caught exception reading " + CLUSTER_DEFINITION_FILE + ".";
      throw new IllegalStateException(msg, e);
    }
  }
}
