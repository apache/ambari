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

package org.apache.ambari.server;

import java.util.List;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.MpackHostStateDAO;
import org.apache.ambari.server.orm.entities.MpackHostStateEntity;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Is executed on server start.
 * Checks server state and recovers it to valid if required.
 */
public class StateRecoveryManager {

  private static final Logger LOG = LoggerFactory.getLogger(StateRecoveryManager.class);

  @Inject
  private MpackHostStateDAO mpackHostStateDAO;

  /**
   * Used for looking up {@link Mpack} instances by IDs.
   */
  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  public void doWork() {
    updateManagementPackInstallationState();
  }

  /**
   * Resets any management pack installation states from
   * {@link RepositoryVersionState#INSTALLING} to
   * {@link RepositoryVersionState#INSTALL_FAILED}.
   */
  void updateManagementPackInstallationState() {
    List<MpackHostStateEntity> mpackHostStates = mpackHostStateDAO.findAll();
    for (MpackHostStateEntity mpackHostState : mpackHostStates) {
      if (mpackHostState.getState() == RepositoryVersionState.INSTALLING) {
        mpackHostState.setState(RepositoryVersionState.INSTALL_FAILED);

        Mpack mpack = ambariMetaInfo.getMpack(mpackHostState.getMpackId());

        String msg = String.format(
                "The installation state of management pack %s on host %s was set from %s to %s",
            mpack.getName(),
                mpackHostState.getHostName(),
                RepositoryVersionState.INSTALLING,
                RepositoryVersionState.INSTALL_FAILED);
        LOG.warn(msg);

        mpackHostStateDAO.merge(mpackHostState);
      }
    }
  }
}
