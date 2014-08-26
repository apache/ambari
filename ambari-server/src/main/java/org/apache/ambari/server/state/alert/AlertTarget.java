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
 * The {@link AlertTarget} class represents a dispatch mechanism and audience
 * that will receive information about alerts int he system.
 */
public class AlertTarget {
  private String m_id;
  private String m_name;
  private String m_description;
  private String m_notificationType;
  private String m_properties;
  private List<AlertGroup> m_groups;

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
   * @return the description
   */
  public String getDescription() {
    return m_description;
  }

  /**
   * @param description
   *          the description to set
   */
  public void setDescription(String description) {
    m_description = description;
  }

  /**
   * @return the notificationType
   */
  public String getNotificationType() {
    return m_notificationType;
  }

  /**
   * @param notificationType
   *          the notificationType to set
   */
  public void setNotificationType(String notificationType) {
    m_notificationType = notificationType;
  }

  /**
   * @return the properties
   */
  public String getProperties() {
    return m_properties;
  }

  /**
   * @param properties
   *          the properties to set
   */
  public void setProperties(String properties) {
    m_properties = properties;
  }

  /**
   * @return the groups
   */
  public List<AlertGroup> getGroups() {
    return m_groups;
  }

  /**
   * @param groups
   *          the groups to set
   */
  public void setGroups(List<AlertGroup> groups) {
    m_groups = groups;
  }
}
