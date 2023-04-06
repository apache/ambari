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
package org.apache.ambari.server.controller;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.checks.CheckDescription;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

/**
 * Represents a prerequisite check request.
 */
public class PrereqCheckRequest {
  private String m_clusterName;
  private StackId m_sourceStackId;
  private RepositoryVersionEntity m_targetRepositoryVersion;
  private PrerequisiteCheckConfig m_prereqCheckConfig;

  private UpgradeType m_upgradeType;

  private Map<CheckDescription, PrereqCheckStatus> m_results = new HashMap<>();
  private boolean m_revert = false;


  public PrereqCheckRequest(String clusterName, UpgradeType upgradeType) {
    m_clusterName = clusterName;
    m_upgradeType = upgradeType;
  }

  /**
   * Construct a request to performs checks before an Upgrade.
   * The default type is Rolling.
   * @param clusterName
   */
  public PrereqCheckRequest(String clusterName) {
    this(clusterName, UpgradeType.ROLLING);
  }

  public String getClusterName() {
    return m_clusterName;
  }

  public UpgradeType getUpgradeType() {
    return m_upgradeType;
  }

  /**
   * Sets the result of a check.
   * @param description the description
   * @param status      the status result
   */
  public void addResult(CheckDescription description, PrereqCheckStatus status) {
    m_results.put(description, status);
  }

  /**
   * Gets the result of a check of the supplied description
   * @param description the description
   * @return the return value, or {@code null} if it has not been run
   */
  public PrereqCheckStatus getResult(CheckDescription description) {
    return m_results.get(description);
  }

  /**
   * Gets the cluster's current stack before upgrade.
   *
   * @return the sourceStackId the source stack ID.
   */
  public StackId getSourceStackId() {
    return m_sourceStackId;
  }

  /**
   * Sets the cluster's current stack before upgrade.
   *
   * @param sourceStackId
   *          the sourceStackId to set
   */
  public void setSourceStackId(StackId sourceStackId) {
    m_sourceStackId = sourceStackId;
  }

  /**
   * Gets the target repository of the upgrade.
   *
   * @return the target repository.
   */
  public RepositoryVersionEntity getTargetRepositoryVersion() {
    return m_targetRepositoryVersion;
  }

  /**
   * Sets the target of the upgrade.
   *
   * @param targetRepositoryVersion
   *          the target repository version
   */
  public void setTargetRepositoryVersion(RepositoryVersionEntity targetRepositoryVersion) {
    m_targetRepositoryVersion = targetRepositoryVersion;
  }

  /**
   * Gets the prerequisite check config
   * @return the prereqCheckConfig
   */
  public PrerequisiteCheckConfig getPrerequisiteCheckConfig() {
    return m_prereqCheckConfig;
  }

  /**
   * Sets the prerequisite check config obtained from the upgrade pack
   * @param prereqCheckConfig The prereqCheckConfig
   */
  public void setPrerequisiteCheckConfig(PrerequisiteCheckConfig prereqCheckConfig) {
    m_prereqCheckConfig = prereqCheckConfig;
  }

  /**
   * @param revert
   *          {@code true} if the check is for a patch reversion
   */
  public void setRevert(boolean revert) {
    m_revert = revert;
  }

  /**
   * @return if the check is for a patch reversion
   */
  public boolean isRevert() {
    return m_revert;
  }
}
