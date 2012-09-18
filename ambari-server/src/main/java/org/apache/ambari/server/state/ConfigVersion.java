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

package org.apache.ambari.server.state;

public class ConfigVersion {

  private String configVersion;

  public ConfigVersion(String configVersion) {
    super();
    this.configVersion = configVersion;
  }

  /**
   * @return the configVersion
   */
  public String getConfigVersion() {
    return configVersion;
  }

  /**
   * @param configVersion the configVersion to set
   */
  public void setConfigVersion(String configVersion) {
    this.configVersion = configVersion;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof ConfigVersion)) {
      return false;
    }
    if (this == object) {
      return true;
    }
    ConfigVersion c = (ConfigVersion) object;
    return configVersion.equals(c.configVersion);
  }

  @Override
  public int hashCode() {
    int result = configVersion != null ? configVersion.hashCode() : 0;
    return result;
  }

}
