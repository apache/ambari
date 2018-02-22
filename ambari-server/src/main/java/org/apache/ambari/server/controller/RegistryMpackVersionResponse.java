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
package org.apache.ambari.server.controller;

import java.util.List;

import org.apache.ambari.server.controller.internal.RegistryMpackVersionResourceProvider;
import org.apache.ambari.server.registry.RegistryMpackDependency;
import org.apache.ambari.server.state.Module;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a registry mpack version response
 */
public class RegistryMpackVersionResponse {
  private Long registryId;
  private String mpackId;
  private String mpackName;
  private String mpackDescription;
  private String mpackVersion;
  private String mpackUri;
  private String mpackDocUri;
  private String mpackLogoUri;
  private List<? extends RegistryMpackDependency> dependencies;
  private List<Module> modules;

  /**
   * Constructor
   * @param registryId        registry id
   * @param mpackId           mpack id
   * @param mpackName         mpack name
   * @param mpackDescription mpack description
   * @param mpackVersion      mpack version
   * @param mpackUri          mpack uri
   * @param mpackDocUri       mpack documentation uri
   * @param mpackLogoUri mpack logo uri
   * @param dependencies      list of mpack dependencies
   * @param modules list of modules in the mpack
   */
  public RegistryMpackVersionResponse(Long registryId, String mpackId, String mpackName, String mpackDescription,
    String mpackVersion, String mpackUri, String mpackDocUri, String mpackLogoUri,
    List<? extends RegistryMpackDependency> dependencies, List<Module> modules) {
    this.registryId = registryId;
    this.mpackId = mpackId;
    this.mpackName = mpackName;
    this.mpackDescription = mpackDescription;
    this.mpackVersion = mpackVersion;
    this.mpackUri = mpackUri;
    this.mpackDocUri = mpackDocUri;
    this.mpackLogoUri = mpackLogoUri;
    this.dependencies = dependencies;
    this.modules = modules;
  }

  public Long getRegistryId() {
    return registryId;
  }

  public String getMpackId() {
    return mpackId;
  }

  public String getMpackName() {
    return mpackName;
  }

  public String getMpackDescription() {
    return mpackDescription;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public String getMpackUri() {
    return mpackUri;
  }

  public String getMpackDocUri() {
    return mpackDocUri;
  }

  public String getMpackLogoUri() {
    return mpackLogoUri;
  }

  public List<? extends RegistryMpackDependency> getDependencies() {
    return dependencies;
  }

  public List<Module> getModules() {
    return modules;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 + getRegistryId().hashCode();
    result = 31 + getMpackName().hashCode();
    result = 31 + getMpackVersion().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RegistryMpackVersionResponse)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RegistryMpackVersionResponse otherResponse = (RegistryMpackVersionResponse) obj;
    return (getRegistryId().equals(otherResponse.getRegistryId())
      && getMpackName().equals(otherResponse.getMpackName())
      && getMpackVersion().equals(otherResponse.getMpackVersion()));
  }

  public interface RegistryMpackVersionResponseWrapper extends ApiModel {
    @ApiModelProperty(name = RegistryMpackVersionResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    RegistryMpackVersionResponse getRegistryMpacVersionkResponse();
  }
}
