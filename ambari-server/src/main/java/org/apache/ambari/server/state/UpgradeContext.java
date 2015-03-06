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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.stack.upgrade.Direction;

/**
 * Used to hold various helper objects required to process an upgrade pack.
 */
public class UpgradeContext {

  private String m_version;
  private Direction m_direction;
  private MasterHostResolver m_resolver;
  private AmbariMetaInfo m_metaInfo;
  private List<ServiceComponentHost> m_unhealthy = new ArrayList<ServiceComponentHost>();
  private Map<String, String> m_serviceNames = new HashMap<String, String>();
  private Map<String, String> m_componentNames = new HashMap<String, String>();

  /**
   * Constructor.
   * @param resolver  the resolver that also references the required cluster
   * @param version   the target version to upgrade to
   * @param direction the direction for the upgrade
   */
  public UpgradeContext(MasterHostResolver resolver, String version,
      Direction direction) {
    m_version = version;
    m_direction = direction;
    m_resolver = resolver;
  }

  /**
   * @return the cluster from the {@link MasterHostResolver}
   */
  public Cluster getCluster() {
    return m_resolver.getCluster();
  }

  /**
   * @return the target version for the upgrade
   */
  public String getVersion() {
    return m_version;
  }

  /**
   * @return the direction of the upgrade
   */
  public Direction getDirection() {
    return m_direction;
  }

  /**
   * @return the resolver
   */
  public MasterHostResolver getResolver() {
    return m_resolver;
  }

  /**
   * @return the metainfo for access to service definitions
   */
  public AmbariMetaInfo getAmbariMetaInfo() {
    return m_metaInfo;
  }

  /**
   * @param metaInfo the metainfo for access to service definitions
   */
  public void setAmbariMetaInfo(AmbariMetaInfo metaInfo) {
    m_metaInfo = metaInfo;
  }

  /**
   * @param unhealthy a list of unhealthy host components
   */
  public void addUnhealthy(List<ServiceComponentHost> unhealthy) {
    m_unhealthy.addAll(unhealthy);
  }

  /**
   * @return a map of host to list of components.
   */
  public Map<String, List<String>> getUnhealthy() {
    Map<String, List<String>> results = new HashMap<String, List<String>>();

    for (ServiceComponentHost sch : m_unhealthy) {
      if (!results.containsKey(sch.getHostName())) {
        results.put(sch.getHostName(), new ArrayList<String>());
      }
      results.get(sch.getHostName()).add(sch.getServiceComponentName());
    }

    return results;
  }

  /**
   * @return the service display name, or the service name if not set
   */
  public String getServiceDisplay(String service) {
    if (m_serviceNames.containsKey(service)) {
      return m_serviceNames.get(service);
    }

    return service;
  }

  /**
   * @return the component display name, or the component name if not set
   */
  public String getComponentDisplay(String service, String component) {
    String key = service + ":" + component;
    if (m_componentNames.containsKey(key)) {
      return m_componentNames.get(key);
    }

    return component;
  }

  /**
   * @param service     the service name
   * @param displayName the display name for the service
   */
  public void setServiceDisplay(String service, String displayName) {
    m_serviceNames.put(service, (displayName == null) ? service : displayName);
  }

  /**
   * @param service     the service name that owns the component
   * @param component   the component name
   * @param displayName the display name for the component
   */
  public void setComponentDisplay(String service, String component, String displayName) {
    String key = service + ":" + component;
    m_componentNames.put(key, displayName);
  }



}
