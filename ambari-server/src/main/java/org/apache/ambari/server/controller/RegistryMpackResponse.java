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

import org.apache.ambari.server.controller.internal.RegistryMpackResourceProvider;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a registry mpack response.
 */
public class RegistryMpackResponse {
  private Long registryId;
  private String mpackName;
  private String mpackDisplayName;
  private String mpackDescription;
  private String mpackLogoUrl;

  /**
   * Constructor
   * @param registryId        registry id
   * @param mpackName         mpack name
   * @param mpackDisplayName      mpack Display Name
   * @param mpackDescription  mpack description
   * @param mpackLogoUrl      mpack logo url
   */
  public RegistryMpackResponse(Long registryId, String mpackName, String mpackDisplayName, String mpackDescription, String mpackLogoUrl) {
    this.registryId = registryId;
    this.mpackName = mpackName;
    this.mpackDisplayName = mpackDisplayName;
    this.mpackDescription = mpackDescription;
    this.mpackLogoUrl = mpackLogoUrl;
  }

  /**
   * Get registry id
   * @return registry id
   */
  public Long getRegistryId() {
    return registryId;
  }

  /**
   * Get mpack name
   * @return mpack name
   */
  public String getMpackName() {
    return mpackName;
  }

  /**
   * Get mpack summary
   * @return
   */
  public String getMpackDisplayName() {return mpackDisplayName;}

  /**
   * Get mpack description
   * @return mpack description
   */
  public String getMpackDescription() {
    return mpackDescription;
  }

  /**
   * Get mpack logo url
   * @return mpack logo url
   */
  public String getMpackLogoUrl() {
    return mpackLogoUrl;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = 1;
    result = 31 + getRegistryId().hashCode();
    result = 31 + getMpackName().hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RegistryMpackResponse)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RegistryMpackResponse otherResponse = (RegistryMpackResponse) obj;
    return (getRegistryId().equals(otherResponse.getRegistryId())
      && getMpackName().equals(otherResponse.getMpackName()));
  }

  public interface RegistryMpackResponseWrapper extends ApiModel {
    @ApiModelProperty(name = RegistryMpackResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    RegistryMpackResponse getRegistryMpackResponse();
  }
}
