/*
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

package org.apache.ambari.server.agent.stomp.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataServiceInfo {
  private String serviceType;
  private String version;
  private Boolean credentialStoreEnabled;
  @JsonProperty("status_commands_timeout")
  private Long statusCommandsTimeout;

  @JsonProperty("service_package_folder")
  private String servicePackageFolder;

  public MetadataServiceInfo(String serviceType, String version, Boolean credentialStoreEnabled, Long statusCommandsTimeout,
                             String servicePackageFolder) {
    this.serviceType = serviceType;
    this.version = version;
    this.credentialStoreEnabled = credentialStoreEnabled;
    this.statusCommandsTimeout = statusCommandsTimeout;
    this.servicePackageFolder = servicePackageFolder;
  }

  public String getServiceType() {
    return serviceType;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Boolean getCredentialStoreEnabled() {
    return credentialStoreEnabled;
  }

  public void setCredentialStoreEnabled(Boolean credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
  }

  public Long getStatusCommandsTimeout() {
    return statusCommandsTimeout;
  }

  public void setStatusCommandsTimeout(Long statusCommandsTimeout) {
    this.statusCommandsTimeout = statusCommandsTimeout;
  }

  public String getServicePackageFolder() {
    return servicePackageFolder;
  }

  public void setServicePackageFolder(String servicePackageFolder) {
    this.servicePackageFolder = servicePackageFolder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MetadataServiceInfo that = (MetadataServiceInfo) o;

    return Objects.equals(serviceType, that.serviceType) &&
      Objects.equals(version, that.version) &&
      Objects.equals(credentialStoreEnabled, that.credentialStoreEnabled) &&
      Objects.equals(statusCommandsTimeout, that.statusCommandsTimeout) &&
      Objects.equals(servicePackageFolder, that.servicePackageFolder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceType, version, credentialStoreEnabled, statusCommandsTimeout, servicePackageFolder);
  }
}
