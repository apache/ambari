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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.checks.CheckDescription;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Represents a prerequisite check request.
 */
public class PrereqCheckRequest {
  private final UpgradePlanEntity m_upgradePlan;
  private PrerequisiteCheckConfig m_prereqCheckConfig;

  private Map<CheckDescription, PrereqCheckStatus> m_results = new HashMap<>();

  /**
   *
   */
  @Inject
  private Clusters m_clusters;

  /**
   * Constructor.
   *
   * @param upgradePlan
   *          the upgrade plan.
   */
  @AssistedInject
  public PrereqCheckRequest(@Assisted UpgradePlanEntity upgradePlan) {
    m_upgradePlan = upgradePlan;
  }

  /**
   * Gets the upgrade plan associated with this upgrade pre-check request.
   *
   * @return the upgrade plan.
   */
  public UpgradePlanEntity getUpgradePlan() {
    return m_upgradePlan;
  }

  /**
   * Gets the cluster name involved in the pre upgrade checks.
   *
   * @return the cluster name.
   */
  public String getClusterName() throws AmbariException {
    return m_clusters.getCluster(m_upgradePlan.getClusterId()).getClusterName();
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
}
