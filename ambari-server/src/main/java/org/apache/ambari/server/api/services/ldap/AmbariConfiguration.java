/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services.ldap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Domain POJO representing generic ambari configuration data.
 */
public class AmbariConfiguration {

  /**
   * The type of the configuration,  eg.: ldap-configuration
   */
  private String type;

  /**
   * Version tag
   */
  private String versionTag;

  /**
   * Version number
   */
  private Integer version;

  /**
   * Created timestamp
   */
  private long createdTs;

  private Set<Map<String, Object>> data = Collections.emptySet();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<Map<String, Object>> getData() {
    return data;
  }

  public void setData(Set<Map<String, Object>> data) {
    this.data = data;
  }

  public String getVersionTag() {
    return versionTag;
  }

  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public long getCreatedTs() {
    return createdTs;
  }

  public void setCreatedTs(long createdTs) {
    this.createdTs = createdTs;
  }
}
