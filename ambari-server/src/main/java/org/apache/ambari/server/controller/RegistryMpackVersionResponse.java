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
import org.apache.ambari.server.registry.RegistryMpackCompatiblity;
import org.apache.ambari.server.registry.RegistryMpackService;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a registry mpack version response
 */
public class RegistryMpackVersionResponse {
  private Long registryId;
  private String mpackName;
  private String mpackVersion;
  private String mpackBuildNumber;
  private String stackId;
  private String mpackUrl;
  private String mpackDocUrl;
  private List<? extends RegistryMpackService> mpackServices;
  private List<? extends RegistryMpackCompatiblity> compatibleMpacks;

  /**
   * Constructor
   * @param registryId        registry id
   * @param mpackName         mpack name
   * @param mpackVersion      mpack version
   * @param mpackBuildNumber  mpack build number
   * @param mpackUrl          mpack download url
   * @param mpackDocUrl       mpack documentation url
   * @param mpackServices     list of mpack services
   * @param compatibleMpacks  list of compatible mpacks
   * @param stackId           stack id of the mpack version
   */
  public RegistryMpackVersionResponse(
    Long registryId, String mpackName, String mpackVersion, String mpackBuildNumber,
    String mpackUrl, String mpackDocUrl,
    List<? extends RegistryMpackService> mpackServices,
    List<? extends RegistryMpackCompatiblity> compatibleMpacks, String stackId) {
    this.registryId = registryId;
    this.mpackName = mpackName;
    this.mpackVersion = mpackVersion;
    this.mpackBuildNumber = mpackBuildNumber;
    this.mpackUrl = mpackUrl;
    this.mpackDocUrl = mpackDocUrl;
    this.mpackServices = mpackServices;
    this.compatibleMpacks = compatibleMpacks;
    this.stackId = stackId;
  }

  public Long getRegistryId() {
    return registryId;
  }

  public String getMpackName() {
    return mpackName;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public String getMpackBuildNumber() {
    return mpackBuildNumber;
  }

  public String getMpackUrl() {
    return mpackUrl;
  }

  public String getMpackDocUrl() {
    return mpackDocUrl;
  }

  public List<? extends RegistryMpackService> getMpackServices() {
    return mpackServices;
  }

  public List<? extends RegistryMpackCompatiblity> getCompatibleMpacks() {
    return compatibleMpacks;
  }


  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
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
