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
package org.apache.ambari.server.agent;

import java.util.List;

import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link AlertDefinitionCommand} class is used to encapsulate the
 * {@link AlertDefinition}s that will be returned to an agent given a requested
 * hash.
 */
public class AlertDefinitionCommand extends AgentCommand {
  @SerializedName("clusterName")
  private final String m_clusterName;

  @SerializedName("hostName")
  private final String m_hostName;

  @SerializedName("hash")
  private final String m_hash;

  @SerializedName("alertDefinitions")
  private final List<AlertDefinition> m_definitions;

  /**
   * Constructor.
   *
   * @param clusterName
   *          the name of the cluster this response is for (
   * @param hostName
   * @param hash
   * @param definitions
   *
   * @see AlertDefinitionHash
   */
  public AlertDefinitionCommand(String clusterName, String hostName,
      String hash, List<AlertDefinition> definitions) {
    super(AgentCommandType.ALERT_DEFINITION_COMMAND);

    m_clusterName = clusterName;
    m_hostName = hostName;
    m_hash = hash;
    m_definitions = definitions;
  }

  /**
   *
   */
  @Override
  public AgentCommandType getCommandType() {
    return AgentCommandType.ALERT_DEFINITION_COMMAND;
  }

  /**
   * Gets the global hash for all alert definitions for a given host.
   *
   * @return the hash (never {@code null}).
   */
  public String getHash() {
    return m_hash;
  }

  /**
   * Gets the alert definitions
   *
   * @return
   */
  public List<AlertDefinition> getAlertDefinitions() {
    return m_definitions;
  }

  /**
   * Gets the name of the cluster.
   *
   * @return the cluster name (not {@code null}).
   */
  public String getClusterName() {
    return m_clusterName;
  }

  /**
   * Gets the host name.
   *
   * @return the host name (not {@code null}).
   */
  public String getHostName() {
    return m_hostName;
  }
}
