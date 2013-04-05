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

/**
 * This class encapsulates a configuration update request.
 * The configuration properties are grouped at service level. It is assumed that
 * different components of a service don't overload same property name.
 */
public class ConfigurationResponse {

  private final String clusterName;

  private final String type;

  private String versionTag;

  private Map<String, String> configs;


  public ConfigurationResponse(String clusterName,
                               String type, String versionTag,
                               Map<String, String> configs) {
    super();
    this.clusterName = clusterName;
    this.configs = configs;
    this.type = type;
    this.versionTag = versionTag;
    this.configs = configs;
  }


  /**
   * @return the versionTag
   */
  public String getVersionTag() {
    return versionTag;
  }

  /**
   * @param versionTag the versionTag to set
   */
  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }

  /**
   * @return the configs
   */
  public Map<String, String> getConfigs() {
    return configs;
  }

  /**
   * @param configs the configs to set
   */
  public void setConfigs(Map<String, String> configs) {
    this.configs = configs;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigurationResponse that =
        (ConfigurationResponse) o;

    if (clusterName != null ?
        !clusterName.equals(that.clusterName) : that.clusterName != null) {
      return false;
    }
    if (type != null ?
        !type.equals(that.type) : that.type != null) {
      return false;
    }
    if (versionTag != null ?
        !versionTag.equals(that.versionTag) : that.versionTag != null){
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 71 * result + (type != null ? type.hashCode() : 0);
    result = 71 * result + (versionTag != null ? versionTag.hashCode():0);
    return result;
  }

}
