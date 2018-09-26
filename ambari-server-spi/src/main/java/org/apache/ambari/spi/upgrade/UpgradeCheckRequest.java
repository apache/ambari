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
package org.apache.ambari.spi.upgrade;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a request to run the upgrade checks before an upgrade begins.
 */
public class UpgradeCheckRequest {
  private String m_clusterName;
  private Map<String, String> m_configurations;

  private UpgradeType m_upgradeType;

  private Map<CheckDescription, UpgradeCheckStatus> m_results = new HashMap<>();
  private boolean m_revert = false;


  /**
   * Constructor.
   *
   * @param clusterName
   *          the name of the cluster.
   * @param upgradeType
   *          the type of the upgrade.
   * @param configurations
   *          any configurations specified in the upgrade pack which can be used
   *          to when
   */
  public UpgradeCheckRequest(String clusterName, UpgradeType upgradeType,
      Map<String, String> configurations) {
    m_clusterName = clusterName;
    m_upgradeType = upgradeType;
    m_configurations = configurations;
  }

  /**
   * Construct a request to performs checks before an Upgrade.
   * The default type is Rolling.
   * @param clusterName
   */
  public UpgradeCheckRequest(String clusterName) {
    this(clusterName, UpgradeType.ROLLING, new HashMap<String, String>());
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
  public void addResult(CheckDescription description, UpgradeCheckStatus status) {
    m_results.put(description, status);
  }

  /**
   * Gets the result of a check of the supplied description
   * @param description the description
   * @return the return value, or {@code null} if it has not been run
   */
  public UpgradeCheckStatus getResult(CheckDescription description) {
    return m_results.get(description);
  }

  /**
   * Gets the prerequisite check config
   * @return the prereqCheckConfig
   */
  public Map<String, String> getConfigurations() {
    return m_configurations;
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
