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
package org.apache.ambari.server.state.alert;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Singleton;

/**
 * The {@link AggregateDefinitionMapping} is used to keep an in-memory mapping
 * of all of the {@link AlertDefinition}s that have aggregate definitions
 * associated with them.
 */
@Singleton
public final class AggregateDefinitionMapping {
  /**
   * In-memory mapping of cluster ID to definition name / aggregate definition.
   * This is used for fast lookups when receiving events.
   */
  private Map<Long, Map<String, AlertDefinition>> m_aggregateMap = new HashMap<Long, Map<String, AlertDefinition>>();

  /**
   * Constructor.
   *
   */
  public AggregateDefinitionMapping() {
  }

  /**
   * Gets an aggregate definition based on a given alert definition name.
   *
   * @param clusterId
   *          the ID of the cluster that the definition is bound to.
   * @param name
   *          the unique name of the definition.
   * @return the aggregate definition, or {@code null} if none.
   */
  public AlertDefinition getAggregateDefinition(long clusterId, String name) {
    Long id = Long.valueOf(clusterId);
    if (!m_aggregateMap.containsKey(id)) {
      return null;
    }

    if (!m_aggregateMap.get(id).containsKey(name)) {
      return null;
    }

    return m_aggregateMap.get(id).get(name);
  }

  /**
   * Adds a mapping for a new aggregate definition.
   *
   * @param clusterId
   *          the ID of the cluster that the definition is bound to.
   * @param name
   *          the unique name of the definition.
   */
  public void addAggregateType(long clusterId, AlertDefinition definition) {
    Long id = Long.valueOf(clusterId);

    if (!m_aggregateMap.containsKey(id)) {
      m_aggregateMap.put(id, new HashMap<String, AlertDefinition>());
    }

    Map<String, AlertDefinition> map = m_aggregateMap.get(id);

    AggregateSource as = (AggregateSource) definition.getSource();

    map.put(as.getAlertName(), definition);
  }
}
