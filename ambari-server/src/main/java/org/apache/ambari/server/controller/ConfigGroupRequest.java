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
package org.apache.ambari.server.controller;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.Config;

public class ConfigGroupRequest {
  private Long id;
  private String clusterName;
  private String groupName;
  private String tag;
  private String description;
  private String serviceConfigVersionNote;
  private Set<String> hosts;
  private Map<String, Config> configs;

  public ConfigGroupRequest(Long id, String clusterName, String groupName,
                            String tag, String description, Set<String> hosts,
                            Map<String, Config> configs) {
    this.id = id;
    this.clusterName = clusterName;
    this.groupName = groupName;
    this.tag = tag;
    this.description = description;
    this.hosts = hosts;
    this.configs = configs;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Set<String> getHosts() {
    return hosts;
  }

  public void setHosts(Set<String> hosts) {
    this.hosts = hosts;
  }

  public Map<String, Config> getConfigs() {
    return configs;
  }

  public void setConfigs(Map<String, Config> configs) {
    this.configs = configs;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getServiceConfigVersionNote() {
    return serviceConfigVersionNote;
  }

  public void setServiceConfigVersionNote(String serviceConfigVersionNote) {
    this.serviceConfigVersionNote = serviceConfigVersionNote;
  }
}
