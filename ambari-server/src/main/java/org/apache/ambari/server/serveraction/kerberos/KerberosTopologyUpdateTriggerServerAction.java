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

package org.apache.ambari.server.serveraction.kerberos;

import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * KerberosTopologyUpdateTriggerServerAction is an implementation of
 * ServerAction that triggers a topology update event in order to update
 * topology cache on agent side with the appropriate key-value parameters (i.e.
 * <code>unlimited_key_jce_require</code> in <code>componentLevelParams</code>)
 * when enabling Kerberos
 */
public class KerberosTopologyUpdateTriggerServerAction extends AbstractServerAction {

  private final static Logger LOG = LoggerFactory.getLogger(KerberosTopologyUpdateTriggerServerAction.class);

  @Inject
  private Provider<TopologyHolder> topologyHolderProvider;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    CommandReport commandReport = null;
    try {
      final TopologyHolder topologyHolder = topologyHolderProvider.get();
      final TopologyUpdateEvent updateEvent = topologyHolder.getCurrentData();
      topologyHolder.updateData(updateEvent);
    } catch (Exception e) {
      String message = "Could not send topology update event when enabling kerberos";
      actionLog.writeStdErr(message);
      LOG.error(message, e);
      commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
    }
    return commandReport == null ? createCompletedCommandReport() : commandReport;
  }
}
