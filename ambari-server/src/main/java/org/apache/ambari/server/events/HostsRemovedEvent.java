/*
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
package org.apache.ambari.server.events;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.state.Cluster;

/**
 * The {@link HostsRemovedEvent} class is fired when the hosts are removed from the
 * cluster.
 */
public class HostsRemovedEvent extends AmbariEvent {

  /**
   * The clusters that the removed hosts belonged to.
   */
  private final Set<Cluster> m_clusters;

  /**
   * Removed hosts.
   */
  private final Set<String> m_hosts;

  private final Map<String, Set<String>> componentsPerHost;

  /**
   * Constructor.
   * @param hosts
   * @param clusters
   * @param componentsPerHost
   */
  public HostsRemovedEvent(Set<String> hosts, Set<Cluster> clusters, Map<String, Set<String>> componentsPerHost) {
    super(AmbariEventType.HOST_REMOVED);
    m_clusters = clusters;
    m_hosts = hosts;
    this.componentsPerHost = componentsPerHost;
  }

  /**
   * The clusters that the hosts belonged to.
   *
   * @return the clusters, or an empty set.
   */
  public Set<Cluster> getClusters() {
    if (null == m_clusters) {
      return Collections.emptySet();
    }

    return m_clusters;
  }

  /**
   * Removed hosts.
   * @return
   */
  public Set<String> getHostNames() {
    if (null == m_hosts) {
      return Collections.emptySet();
    }

    return m_hosts;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("HostsRemovedEvent{");
    sb.append("m_clusters=").append(m_clusters);
    sb.append(", m_hosts=").append(m_hosts);
    sb.append('}');
    return sb.toString();
  }

  /**
   * @return true if any of the deleted host had the given component
   */
  public boolean hasComponent(Role component) {
    return flatten(componentsPerHost.values()).contains(component.name());
  }

  private static Set<String> flatten(Collection<Set<String>> values) {
    return values.stream().flatMap(Collection::stream).collect(toSet());
  }
}
