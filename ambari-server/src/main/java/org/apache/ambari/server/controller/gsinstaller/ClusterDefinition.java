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

import org.apache.ambari.server.utils.Closeables;

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

  private final GSInstallerStateProvider stateProvider;
  private String clusterName;
  private String versionId;

  /**
   * Index of host names to host component state.
   */
  private final Map<String, Set<HostComponentState>> hostStateMap = new HashMap<String, Set<HostComponentState>>();

  /**
   * Index of service names to host component state.
   */
  private final Map<String, Set<HostComponentState>> serviceStateMap = new HashMap<String, Set<HostComponentState>>();

  /**
   * Index of component names to host component state.
   */
  private final Map<String, Set<HostComponentState>> componentStateMap = new HashMap<String, Set<HostComponentState>>();

  /**
   * Index of host component names to host component state.
   */
  private final Map<String, HostComponentState> hostComponentStateMap = new HashMap<String, HostComponentState>();

  /**
   * Expiry for the health value.
   */
  private static final int DEFAULT_STATE_EXPIRY = 15000;

  /**
   * Component name mapping to account for differences in what is provided by the gsInstaller
   * and what is expected by the Ambari providers.
   */
  private static final Map<String, String> componentNameMap = new HashMap<String, String>();

  static {
    componentNameMap.put("GANGLIA", "GANGLIA_SERVER");
  }

  // ----- Constructors ------------------------------------------------------

  /**
   * Create a cluster definition.
   *
   * @param stateProvider  the state provider
   */
  public ClusterDefinition(GSInstallerStateProvider stateProvider) {
    this(stateProvider, DEFAULT_STATE_EXPIRY);
  }

  /**
   * Create a cluster definition.
   *
   * @param stateProvider  the state provider
   * @param stateExpiry    the state expiry
   */
  public ClusterDefinition(GSInstallerStateProvider stateProvider, int stateExpiry) {
    this.stateProvider = stateProvider;
    this.clusterName   = DEFAULT_CLUSTER_NAME;
    this.versionId     = DEFAULT_VERSION_ID;
    readClusterDefinition();
    setHostComponentState(stateExpiry);
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

  /**
   * Get the host state from the given host name.
   *
   * @param hostName  the host name
   *
   * @return the host state
   */
  public String getHostState(String hostName) {
    return isHealthy(hostStateMap.get(hostName)) ? "HEALTHY" : "INIT";
  }

  /**
   * Get the service state from the given service name.
   *
   * @param serviceName  the service name
   *
   * @return the service state
   */
  public String getServiceState(String serviceName) {
    return isHealthy(serviceStateMap.get(serviceName)) ? "STARTED" : "INIT";
  }

  /**
   * Get the component state from the give service name and component name.
   *
   * @param serviceName    the service name
   * @param componentName  the component name
   *
   * @return the component state
   */
  public String getComponentState(String serviceName, String componentName) {
    return isHealthy(componentStateMap.get(getComponentKey(serviceName, componentName))) ? "STARTED" : "INIT";
  }

  /**
   * Get the host component name from the given host name, service name and component name.
   *
   * @param hostName       the host name
   * @param serviceName    the service name
   * @param componentName  the component name
   *
   * @return the host component state
   */
  public String getHostComponentState(String hostName, String serviceName, String componentName) {
    return isHealthy(hostComponentStateMap.get(getHostComponentKey(hostName, serviceName, componentName))) ? "STARTED" : "INIT";
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Read the gsInstaller cluster definition file.
   */
  private void readClusterDefinition() {
    InputStream is = null;
    try {
      is = this.getClass().getClassLoader().getResourceAsStream(CLUSTER_DEFINITION_FILE);
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

          // translate the component name if required
          if (componentNameMap.containsKey(componentName)) {
            componentName = componentNameMap.get(componentName);
          }

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
    } finally {
      Closeables.closeSilently(is);
    }
  }

  /**
   * Set the host component state maps.
   */
  private void setHostComponentState(int stateExpiry) {
    for (Map.Entry<String, Map<String, Set<String>>> serviceEntry : hostComponents.entrySet()) {
      String serviceName = serviceEntry.getKey();

      for (Map.Entry<String, Set<String>> hostEntry : serviceEntry.getValue().entrySet()) {
        String hostName = hostEntry.getKey();

        for (String componentName : hostEntry.getValue()) {

          HostComponentState state = new HostComponentState(hostName, componentName, stateExpiry);

          // add state to hosts
          addState(hostName, hostStateMap, state);

          // add state to services
          addState(serviceName, serviceStateMap, state);

          // add state to components
          addState(getComponentKey(serviceName, componentName), componentStateMap, state);

          // add state to host components
          hostComponentStateMap.put(getHostComponentKey(hostName, serviceName, componentName), state);
        }
      }
    }
  }

  /**
   * Add the given host component state object to the given map of state objects.
   *
   * @param hostName  the host name
   * @param stateMap  the map of state objects
   * @param state     the state
   */
  private static void addState(String hostName, Map<String, Set<HostComponentState>> stateMap, HostComponentState state) {
    Set<HostComponentState> states = stateMap.get(hostName);
    if (states == null) {
      states = new HashSet<HostComponentState>();
      stateMap.put(hostName, states);
    }
    states.add(state);
  }

  /**
   * Get a key from the given service name and component name.
   *
   * @param serviceName    the service name
   * @param componentName  the component name
   *
   * @return the key
   */
  private String getComponentKey(String serviceName, String componentName) {
    return serviceName + "." + componentName;
  }

  /**
   * Get a key from the given host name, service name and component name.
   *
   * @param hostName       the host name
   * @param serviceName    the service name
   * @param componentName  the component name
   *
   * @return the key
   */
  private String getHostComponentKey(String hostName, String serviceName, String componentName) {
    return hostName + "." + serviceName + "." + componentName;
  }

  /**
   * Determine whether or not the host components associated
   * with the given states are healthy.
   *
   * @param states  the states
   *
   * @return true if the associated host components are healthy
   */
  private boolean isHealthy(Set<HostComponentState> states) {
    if (states != null) {
      for (HostComponentState state : states) {
        if (!state.isHealthy()) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Determine whether or not the host component associated
   * with the given state is healthy.
   *
   * @param state  the state
   *
   * @return true if the associated host component is healthy
   */
  private boolean isHealthy(HostComponentState state) {
    return state == null || state.isHealthy();
  }


  // ----- inner classes -----------------------------------------------------

  /**
   * A state object used to check the health of a host component.
   */
  private class HostComponentState {
    private final String hostName;
    private final String componentName;
    private final int expiry;
    private boolean healthy = true;
    private long lastAccess;

    // ----- Constructor -----------------------------------------------------

    /**
     * Constructor.
     *
     * @param hostName       the host name
     * @param componentName  the component name
     */
    HostComponentState(String hostName, String componentName, int expiry) {
      this.hostName      = hostName;
      this.componentName = componentName;
      this.expiry        = expiry;
    }

    /**
     * Determine whether or not the associated host component is healthy.
     *
     * @return true if the associated host component is healthy
     */
    public boolean isHealthy() {
      if (System.currentTimeMillis() - lastAccess > expiry) {
        // health value has expired... get it again
        healthy = stateProvider.isHealthy(hostName, componentName);
        this.lastAccess = System.currentTimeMillis();
      }
      return healthy;
    }
  }
}
