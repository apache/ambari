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

import java.io.File;
import java.util.Collection;
import java.util.Map;


public class StackVersionResponse {

  private String stackName;
  private String stackVersion;
  private String minUpgradeVersion;
  private boolean active;
  private String parentVersion;
  private Map<String, Map<String, Map<String, String>>> configTypes;

  /**
   * A File pointing to the stack-level Kerberos descriptor file
   *
   * This may be null if a relevant file is not available.
   */
  private File stackKerberosDescriptorFile;

  /**
   * A Collection of Files pointing to the service-level Kerberos descriptor files
   *
   * This may be null or empty if no relevant files are available.
   */
  private Collection<File> serviceKerberosDescriptorFiles;

  public StackVersionResponse(String stackVersion, String minUpgradeVersion,
                              boolean active, String parentVersion,
                              Map<String, Map<String, Map<String, String>>> configTypes,
                              File stackKerberosDescriptorFile,
                              Collection<File> serviceKerberosDescriptorFiles) {
    setStackVersion(stackVersion);
    setMinUpgradeVersion(minUpgradeVersion);
    setActive(active);
    setParentVersion(parentVersion);
    setConfigTypes(configTypes);
    setKerberosDescriptorFile(stackKerberosDescriptorFile);
    setServiceKerberosDescriptorFiles(serviceKerberosDescriptorFiles);
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  public String getStackVersion() {
    return stackVersion;
  }

  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  public String getMinUpgradeVersion() {
    return minUpgradeVersion;
  }

  public void setMinUpgradeVersion(String minUpgradeVersion) {
    this.minUpgradeVersion = minUpgradeVersion;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getParentVersion() {
    return parentVersion;
  }

  public void setParentVersion(String parentVersion) {
    this.parentVersion = parentVersion;
  }
  public Map<String, Map<String, Map<String, String>>> getConfigTypes() {
    return configTypes;
  }
  public void setConfigTypes(
      Map<String, Map<String, Map<String, String>>> configTypes) {
    this.configTypes = configTypes;
  }

  /**
   * Gets a File pointing to the stack-level Kerberos descriptor
   *
   * @return a File pointing to the stack-level Kerberos descriptor, or null if no relevant file is
   * available
   */
  public File getStackKerberosDescriptorFile() {
    return stackKerberosDescriptorFile;
  }

  /**
   * Sets the stack-level Kerberos descriptor File
   *
   * @param stackKerberosDescriptorFile a File pointing to the stack-level Kerberos descriptor
   */
  public void setKerberosDescriptorFile(File stackKerberosDescriptorFile) {
    this.stackKerberosDescriptorFile = stackKerberosDescriptorFile;
  }

  /**
   * Gets the Collection of Files pointing to the stack-specific service-level Kerberos descriptor
   * files
   *
   * @return a Collection of Files pointing to the stack-specific service-level Kerberos descriptor
   * files, or null if no relevant files are available
   */
  public Collection<File> getServiceKerberosDescriptorFiles() {
    return serviceKerberosDescriptorFiles;
  }

  /**
   * Sets the Collection of stack-specific service-level Kerberos descriptor Files
   *
   * @param serviceKerberosDescriptorFiles a Collection of stack-specific service-level Kerberos
   *                                       descriptor Files
   */
  public void setServiceKerberosDescriptorFiles(Collection<File> serviceKerberosDescriptorFiles) {
    this.serviceKerberosDescriptorFiles = serviceKerberosDescriptorFiles;
  }
}
