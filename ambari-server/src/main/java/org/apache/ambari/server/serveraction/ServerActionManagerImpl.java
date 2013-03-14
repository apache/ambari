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

package org.apache.ambari.server.serveraction;

import com.google.inject.Singleton;
import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Server action manager implementation.
 */
@Singleton
public class ServerActionManagerImpl implements ServerActionManager {

  private final static Logger LOG =
      LoggerFactory.getLogger(ServerActionManagerImpl.class);

  private Clusters clusters;

  @Inject
  public ServerActionManagerImpl(Clusters clusters) {
    this.clusters = clusters;
  }

  @Override
  public void executeAction(String actionName, Map<String, String> payload)
      throws AmbariException {
    LOG.info("Executing server action : "
        + actionName + " with payload "
        + payload);

    if (actionName.equals(ServerAction.Command.FINALIZE_UPGRADE)) {
      updateClusterStackVersion(payload);
    } else {
      throw new AmbariException("Unsupported action " + actionName);
    }
  }

  private void updateClusterStackVersion(Map<String, String> payload) throws AmbariException {
    if (payload == null
        || !payload.containsKey(ServerAction.PayloadName.CLUSTER_NAME)
        || !payload.containsKey(ServerAction.PayloadName.CURRENT_STACK_VERSION)) {
      throw new AmbariException("Invalid payload.");
    }

    StackId currentStackId = new StackId(payload.get(ServerAction.PayloadName.CURRENT_STACK_VERSION));
    final Cluster cluster = clusters.getCluster(payload.get(ServerAction.PayloadName.CLUSTER_NAME));
    cluster.setCurrentStackVersion(currentStackId);
    cluster.refresh();
  }
}
