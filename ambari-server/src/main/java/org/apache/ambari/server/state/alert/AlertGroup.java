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
package org.apache.ambari.server.state.alert;

import java.util.List;

/**
 * The {@link AlertGroup} class represents a grouping of {@link AlertDefinition}
 * instances as well as the targets that will be invoked when an alert is
 * triggered.
 */
public class AlertGroup {
  private String m_id;
  private String m_name;
  private String m_clusterName;
  private boolean m_isDefault;
  private List<AlertDefinition> m_definitions;
  private List<AlertTarget> m_targets;

  /**
   * @return the id
   */
  public String getId() {
    return m_id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(String id) {
    m_id = id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return m_name;
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName(String name) {
    m_name = name;
  }

  /**
   * @return the clusterName
   */
  public String getClusterName() {
    return m_clusterName;
  }

  /**
   * @param clusterName
   *          the clusterName to set
   */
  public void setClusterName(String clusterName) {
    m_clusterName = clusterName;
  }

  /**
   * @return the isDefault
   */
  public boolean isDefault() {
    return m_isDefault;
  }

  /**
   * @param isDefault
   *          the isDefault to set
   */
  public void setDefault(boolean isDefault) {
    m_isDefault = isDefault;
  }

  /**
   * @return the definitions
   */
  public List<AlertDefinition> getDefinitions() {
    return m_definitions;
  }

  /**
   * @param definitions
   *          the definitions to set
   */
  public void setDefinitions(List<AlertDefinition> definitions) {
    m_definitions = definitions;
  }

  /**
   * @return the targets
   */
  public List<AlertTarget> getTargets() {
    return m_targets;
  }

  /**
   * @param targets
   *          the targets to set
   */
  public void setTargets(List<AlertTarget> targets) {
    m_targets = targets;
  }
}
