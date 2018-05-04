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
package org.apache.ambari.server.topology;

import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;
import org.apache.ambari.server.agent.stomp.dto.TopologyComponent;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.events.TopologyUpdateEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class TopologyDeleteFormer {

  @Inject
  private Provider<TopologyHolder> m_topologyHolder;

  public void processDeleteMetaDataException(DeleteHostComponentStatusMetaData metaData) throws AmbariException {
    if (metaData.getAmbariException() != null) {

      TopologyUpdateEvent topologyUpdateEvent = new TopologyUpdateEvent(
          createUpdateFromDeleteMetaData(metaData),
          TopologyUpdateEvent.EventType.DELETE
      );
      m_topologyHolder.get().updateData(topologyUpdateEvent);

      throw metaData.getAmbariException();
    }
  }
  public void processDeleteMetaData(DeleteHostComponentStatusMetaData metaData) throws AmbariException {
    TopologyUpdateEvent topologyUpdateEvent = new TopologyUpdateEvent(
        createUpdateFromDeleteMetaData(metaData),
        TopologyUpdateEvent.EventType.DELETE
    );
    m_topologyHolder.get().updateData(topologyUpdateEvent);
  }

  public void processDeleteCluster(String clusterId) throws AmbariException {
    TreeMap<String, TopologyCluster> topologyUpdates = new TreeMap<>();
    topologyUpdates.put(clusterId, new TopologyCluster());
    TopologyUpdateEvent topologyUpdateEvent = new TopologyUpdateEvent(
        topologyUpdates,
        TopologyUpdateEvent.EventType.DELETE
    );
    m_topologyHolder.get().updateData(topologyUpdateEvent);
  }

  public TreeMap<String, TopologyCluster> createUpdateFromDeleteMetaData(DeleteHostComponentStatusMetaData metaData) {
    TreeMap<String, TopologyCluster> topologyUpdates = new TreeMap<>();

    for (DeleteHostComponentStatusMetaData.HostComponent hostComponent : metaData.getRemovedHostComponents()) {
      TopologyComponent deletedComponent = TopologyComponent.newBuilder()
          .setComponentName(hostComponent.getComponentName())
          .setServiceName(hostComponent.getServiceName())
          .setVersion(hostComponent.getVersion())
          .setHostIds(new HashSet<>(Arrays.asList(hostComponent.getHostId())))
          .setHostNames(new HashSet<>(Arrays.asList(hostComponent.getHostName())))
          .setLastComponentState(hostComponent.getLastComponentState())
          .build();

      String clusterId = hostComponent.getClusterId();
      if (!topologyUpdates.containsKey(clusterId)) {
        topologyUpdates.put(clusterId, new TopologyCluster());
      }

      if (!topologyUpdates.get(clusterId).getTopologyComponents().contains(deletedComponent)) {
        topologyUpdates.get(clusterId).addTopologyComponent(deletedComponent);
      } else {
        topologyUpdates.get(clusterId).getTopologyComponents()
            .stream().filter(t -> t.equals(deletedComponent))
            .forEach(t -> t.addHostName(hostComponent.getHostName()));
        topologyUpdates.get(clusterId).getTopologyComponents()
            .stream().filter(t -> t.equals(deletedComponent))
            .forEach(t -> t.addHostId(hostComponent.getHostId()));
      }
    }

    return topologyUpdates;
  }
}
