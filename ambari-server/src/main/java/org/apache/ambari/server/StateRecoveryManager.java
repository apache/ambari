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

package org.apache.ambari.server;

import com.google.inject.Inject;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Is executed on server start.
 * Checks server state and recovers it to valid if required.
 */
public class StateRecoveryManager {

  private static Logger LOG = LoggerFactory.getLogger(StateRecoveryManager.class);

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private ClusterVersionDAO clusterVersionDAO;


  public void doWork() {
    checkHostAndClusterVersions();
  }


  void checkHostAndClusterVersions() {
    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();
    for (HostVersionEntity hostVersion : hostVersions) {
      if (hostVersion.getState().equals(RepositoryVersionState.INSTALLING)) {
        hostVersion.setState(RepositoryVersionState.INSTALL_FAILED);
        String msg = String.format(
                "Recovered state of host version %s on host %s from %s to %s",
                hostVersion.getRepositoryVersion().getDisplayName(),
                hostVersion.getHostName(),
                RepositoryVersionState.INSTALLING,
                RepositoryVersionState.INSTALL_FAILED);
        LOG.warn(msg);
        hostVersionDAO.merge(hostVersion);
      }
    }

    List<ClusterVersionEntity> clusterVersions = clusterVersionDAO.findAll();
    for (ClusterVersionEntity clusterVersion : clusterVersions) {
      if (clusterVersion.getState().equals(RepositoryVersionState.INSTALLING)) {
        clusterVersion.setState(RepositoryVersionState.INSTALL_FAILED);
        String msg = String.format(
                "Recovered state of cluster version %s for cluster %s from %s to %s",
                clusterVersion.getRepositoryVersion().getDisplayName(),
                clusterVersion.getClusterEntity().getClusterName(),
                RepositoryVersionState.INSTALLING,
                RepositoryVersionState.INSTALL_FAILED);
        LOG.warn(msg);
        clusterVersionDAO.merge(clusterVersion);
      }
    }
  }


}
