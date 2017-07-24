/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.registry;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 *  Mpack version recommenation
 */
public class MpackEntry {
  private String mpackName;
  private String mpackVersion;
  private RegistryMpackVersion registryMpackVersion;

  public MpackEntry(String mpackName, String mpackVersion) {
    this.mpackName = mpackName;
    this.mpackVersion = mpackVersion;
    this.registryMpackVersion = registryMpackVersion;
  }

  /**
   * Get mpack name
   * @return
   */
  @JsonProperty("mpack_name")
  public String getMpackName() {
    return mpackName;
  }

  /**
   * Get version
   * @return
   */
  @JsonProperty("mpack_version")
  public String getMpackVersion() {
    return mpackVersion;
  }

  /**
   * Set registry mpack version
   * @param registryMpackVersion
   */
  public void setRegistryMpackVersion(RegistryMpackVersion registryMpackVersion) {
    this.registryMpackVersion = registryMpackVersion;
  }

  /**
   * Get registry mpack version
   * @return
   */
  @JsonIgnore
  public RegistryMpackVersion getRegistryMpackVersion() {
    return registryMpackVersion;
  }
}
