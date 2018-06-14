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
package org.apache.ambari.server.serveraction.upgrades;

import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.ServiceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpdateDesiredMpackAction} is used during an upgrade to change the
 * association of a {@link ServiceGroup}'s management pack.
 */
@Experimental(feature = ExperimentalFeature.MPACK_UPGRADES)
public class UpdateDesiredMpackAction extends AbstractUpgradeServerAction {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpdateDesiredMpackAction.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {
    return createCommandReport(-1, HostRoleStatus.FAILED, "{}", "", "");
  }
}
