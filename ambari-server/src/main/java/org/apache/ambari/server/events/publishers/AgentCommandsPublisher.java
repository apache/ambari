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

package org.apache.ambari.server.events.publishers;

import static org.apache.ambari.server.controller.KerberosHelperImpl.CHECK_KEYTABS;
import static org.apache.ambari.server.controller.KerberosHelperImpl.REMOVE_KEYTAB;
import static org.apache.ambari.server.controller.KerberosHelperImpl.SET_KEYTAB;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentCommand;
import org.apache.ambari.server.agent.CancelCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.dto.ExecutionCommandsCluster;
import org.apache.ambari.server.events.ExecutionCommandEvent;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileReader;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.stageutils.KerberosKeytabController;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AgentCommandsPublisher {
  private static final Logger LOG = LoggerFactory.getLogger(AgentCommandsPublisher.class);

  @Inject
  private KerberosKeytabController kerberosKeytabController;

  @Inject
  private Clusters clusters;

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private AgentConfigsHolder agentConfigsHolder;

  public void sendAgentCommand(Multimap<Long, AgentCommand> agentCommands) throws AmbariException {
    if (agentCommands != null && !agentCommands.isEmpty()) {
      Map<Long, TreeMap<String, ExecutionCommandsCluster>> executionCommandsClusters = new TreeMap<>();
      for (Map.Entry<Long, AgentCommand> acHostEntry : agentCommands.entries()) {
        Long hostId = acHostEntry.getKey();
        AgentCommand ac = acHostEntry.getValue();
        populateExecutionCommandsClusters(executionCommandsClusters, hostId, ac);
      }
      for (Map.Entry<Long, TreeMap<String, ExecutionCommandsCluster>> hostEntry : executionCommandsClusters.entrySet()) {
        Long hostId = hostEntry.getKey();
        ExecutionCommandEvent executionCommandEvent = new ExecutionCommandEvent(hostEntry.getValue());
        executionCommandEvent.setHostId(hostId);
        executionCommandEvent.setRequiredConfigTimestamp(agentConfigsHolder
            .initializeDataIfNeeded(hostId, true).getTimestamp());
        STOMPUpdatePublisher.publish(executionCommandEvent);
      }
    }
  }

  public void sendAgentCommand(Long hostId, AgentCommand agentCommand) throws AmbariException {
    Multimap<Long, AgentCommand> agentCommands = ArrayListMultimap.create();
    agentCommands.put(hostId, agentCommand);
    sendAgentCommand(agentCommands);
  }

  private void populateExecutionCommandsClusters(Map<Long, TreeMap<String, ExecutionCommandsCluster>> executionCommandsClusters,
                                            Long hostId, AgentCommand ac) throws AmbariException {
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Sending command string = " + StageUtils.jaxbToString(ac));
      }
    } catch (Exception e) {
      throw new AmbariException("Could not get jaxb string for command", e);
    }
    switch (ac.getCommandType()) {
      case BACKGROUND_EXECUTION_COMMAND:
      case EXECUTION_COMMAND: {
        ExecutionCommand ec = (ExecutionCommand) ac;
        LOG.info("AgentCommandsPublisher.sendCommands: sending ExecutionCommand for host {}, role {}, roleCommand {}, and command ID {}, task ID {}",
            ec.getHostname(), ec.getRole(), ec.getRoleCommand(), ec.getCommandId(), ec.getTaskId());
        Map<String, String> hlp = ec.getCommandParams();
        if (hlp != null) {
          String customCommand = hlp.get("custom_command");
          if (SET_KEYTAB.equalsIgnoreCase(customCommand) || REMOVE_KEYTAB.equalsIgnoreCase(customCommand) || CHECK_KEYTABS.equalsIgnoreCase(customCommand)) {
            LOG.info(String.format("%s called", customCommand));
            try {
              injectKeytab(ec, customCommand, clusters.getHostById(hostId).getHostName());
            } catch (IOException e) {
              throw new AmbariException("Could not inject keytab into command", e);
            }
          }
        }
        String clusterName = ec.getClusterName();
        String clusterId = "-1";
        if (clusterName != null) {
          clusterId = Long.toString(clusters.getCluster(clusterName).getClusterId());
        }
        ec.setClusterId(clusterId);
        prepareExecutionCommandsClusters(executionCommandsClusters, hostId, clusterId);
        executionCommandsClusters.get(hostId).get(clusterId).getExecutionCommands().add((ExecutionCommand) ac);
        break;
      }
      case CANCEL_COMMAND: {
        CancelCommand cc = (CancelCommand) ac;
        String clusterId = Long.toString(hostRoleCommandDAO.findByPK(cc.getTargetTaskId()).getStage().getClusterId());
        prepareExecutionCommandsClusters(executionCommandsClusters, hostId, clusterId);
        executionCommandsClusters.get(hostId).get(clusterId).getCancelCommands().add(cc);
        break;
      }
      default:
        LOG.error("There is no action for agent command ="
            + ac.getCommandType().name());
    }
  }

  private void prepareExecutionCommandsClusters(Map<Long, TreeMap<String, ExecutionCommandsCluster>> executionCommandsClusters,
                                                Long hostId, String clusterId) {
    if (!executionCommandsClusters.containsKey(hostId)) {
      executionCommandsClusters.put(hostId, new TreeMap<>());
    }
    if (!executionCommandsClusters.get(hostId).containsKey(clusterId)) {
      executionCommandsClusters.get(hostId).put(clusterId, new ExecutionCommandsCluster(new ArrayList<>(),
          new ArrayList<>()));
    }
  }

  /**
   * Insert Kerberos keytab details into the ExecutionCommand for the SET_KEYTAB custom command if
   * any keytab details and associated data exists for the target host.
   *
   * @param ec the ExecutionCommand to update
   * @param command a name of the relevant keytab command
   * @param targetHost a name of the host the relevant command is destined for
   * @throws AmbariException
   */
  void injectKeytab(ExecutionCommand ec, String command, String targetHost) throws AmbariException {
    String dataDir = ec.getCommandParams().get(KerberosServerAction.DATA_DIRECTORY);
    KerberosServerAction.KerberosCommandParameters kerberosCommandParameters = new KerberosServerAction.KerberosCommandParameters(ec);
    if(dataDir != null) {
      List<Map<String, String>> kcp = ec.getKerberosCommandParams();

      try {
        Set<ResolvedKerberosKeytab> keytabsToInject = kerberosKeytabController.getFilteredKeytabs((Map<String, Collection<String>>)kerberosCommandParameters.getServiceComponentFilter(), kerberosCommandParameters.getHostFilter(), kerberosCommandParameters.getIdentityFilter());
        for (ResolvedKerberosKeytab resolvedKeytab : keytabsToInject) {
          for(ResolvedKerberosPrincipal resolvedPrincipal: resolvedKeytab.getPrincipals()) {
            String hostName = resolvedPrincipal.getHostName();

            if (targetHost.equalsIgnoreCase(hostName)) {

              if (SET_KEYTAB.equalsIgnoreCase(command)) {
                String keytabFilePath = resolvedKeytab.getFile();

                if (keytabFilePath != null) {

                  String sha1Keytab = DigestUtils.sha256Hex(keytabFilePath);
                  File keytabFile = new File(dataDir + File.separator + hostName + File.separator + sha1Keytab);

                  if (keytabFile.canRead()) {
                    Map<String, String> keytabMap = new HashMap<>();
                    String principal = resolvedPrincipal.getPrincipal();

                    keytabMap.put(KerberosIdentityDataFileReader.HOSTNAME, hostName);
                    keytabMap.put(KerberosIdentityDataFileReader.PRINCIPAL, principal);
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH, keytabFilePath);
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME, resolvedKeytab.getOwnerName());
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS, resolvedKeytab.getOwnerAccess());
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME, resolvedKeytab.getGroupName());
                    keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS, resolvedKeytab.getGroupAccess());

                    BufferedInputStream bufferedIn = new BufferedInputStream(new FileInputStream(keytabFile));
                    byte[] keytabContent = null;
                    try {
                      keytabContent = IOUtils.toByteArray(bufferedIn);
                    } finally {
                      bufferedIn.close();
                    }
                    String keytabContentBase64 = Base64.encodeBase64String(keytabContent);
                    keytabMap.put(KerberosServerAction.KEYTAB_CONTENT_BASE64, keytabContentBase64);

                    kcp.add(keytabMap);
                  }
                }
              } else if (REMOVE_KEYTAB.equalsIgnoreCase(command) || CHECK_KEYTABS.equalsIgnoreCase(command)) {
                Map<String, String> keytabMap = new HashMap<>();
                String keytabFilePath = resolvedKeytab.getFile();

                String principal = resolvedPrincipal.getPrincipal();
                for (Map.Entry<String, String> mappingEntry: resolvedPrincipal.getServiceMapping().entries()) {
                  String serviceName = mappingEntry.getKey();
                  String componentName = mappingEntry.getValue();
                  keytabMap.put(KerberosIdentityDataFileReader.HOSTNAME, hostName);
                  keytabMap.put(KerberosIdentityDataFileReader.SERVICE, serviceName);
                  keytabMap.put(KerberosIdentityDataFileReader.COMPONENT, componentName);
                  keytabMap.put(KerberosIdentityDataFileReader.PRINCIPAL, principal);
                  keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH, keytabFilePath);

                }

                kcp.add(keytabMap);
              }
            }
          }
        }
      } catch (IOException e) {
        throw new AmbariException("Could not inject keytabs to enable kerberos");
      }
      ec.setKerberosCommandParams(kcp);
    }
  }
}
