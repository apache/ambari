/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.agent.stomp;

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.MetadataCluster;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.events.MetadataUpdateEvent;
import org.apache.commons.collections.MapUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MetadataHolder extends AgentClusterDataHolder<MetadataUpdateEvent> {

  @Inject
  private AmbariManagementControllerImpl ambariManagementController;

  @Override
  public MetadataUpdateEvent getCurrentData() throws AmbariException {
    return ambariManagementController.getClustersMetadata();
  }

  @Override
  protected boolean handleUpdate(MetadataUpdateEvent update) throws AmbariException {
    boolean changed = false;
    if (MapUtils.isNotEmpty(update.getMetadataClusters())) {
      for (Map.Entry<String, MetadataCluster> metadataClusterEntry : update.getMetadataClusters().entrySet()) {
        MetadataCluster updatedCluster = metadataClusterEntry.getValue();
        String clusterId = metadataClusterEntry.getKey();
        Map<String, MetadataCluster> clusters = getData().getMetadataClusters();
        if (clusters.containsKey(clusterId)) {
          MetadataCluster cluster = clusters.get(clusterId);
          cluster.getClusterLevelParams().putAll(updatedCluster.getClusterLevelParams());
          cluster.getServiceLevelParams().putAll(updatedCluster.getServiceLevelParams());
          cluster.getStatusCommandsToRun().addAll(updatedCluster.getStatusCommandsToRun());
        } else {
          clusters.put(clusterId, updatedCluster);
        }
        changed = true;
      }
    }
    return changed;
  }

  @Override
  protected MetadataUpdateEvent getEmptyData() {
    return MetadataUpdateEvent.emptyUpdate();
  }
}
