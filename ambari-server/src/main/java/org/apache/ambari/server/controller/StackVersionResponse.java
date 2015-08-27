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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.ambari.server.stack.Validable;


public class StackVersionResponse implements Validable{

  private String minJdk;
  private String maxJdk;

  public String getMinJdk() {
    return minJdk;
  }

  public void setMinJdk(String minJdk) {
    this.minJdk = minJdk;
  }

  public String getMaxJdk() {
    return maxJdk;
  }

  public void setMaxJdk(String maxJdk) {
    this.maxJdk = maxJdk;
  }

  private String stackName;
  private String stackVersion;
  private String minUpgradeVersion;
  private boolean active;
  private boolean valid;
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
  private Set<String> upgradePacks = Collections.emptySet();

  public StackVersionResponse(String stackVersion, String minUpgradeVersion,
                              boolean active, String parentVersion,
                              Map<String, Map<String, Map<String, String>>> configTypes,
                              File stackKerberosDescriptorFile,
                              Collection<File> serviceKerberosDescriptorFiles,
                              Set<String> upgradePacks, boolean valid, Collection errorSet, String minJdk, String maxJdk) {
    setStackVersion(stackVersion);
    setMinUpgradeVersion(minUpgradeVersion);
    setActive(active);
    setParentVersion(parentVersion);
    setConfigTypes(configTypes);
    setKerberosDescriptorFile(stackKerberosDescriptorFile);
    setServiceKerberosDescriptorFiles(serviceKerberosDescriptorFiles);
    setUpgradePacks(upgradePacks);
    setValid(valid);
    setErrors(errorSet);
    setMinJdk(minJdk);
    setMaxJdk(maxJdk);
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }  

  private Set<String> errorSet = new HashSet<String>();
  
  @Override
  public void setErrors(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection getErrors() {
    return errorSet;
  }   

  @Override
  public void setErrors(Collection error) {
    this.errorSet.addAll(error);
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
  
  /**
   * @param upgradePacks the names of the upgrade packs for the stack version 
   */
  public void setUpgradePacks(Set<String> upgradePacks) {
    this.upgradePacks = upgradePacks;
  }
  
  /**
   * @return the upgrade pack names for the stack version
   */
  public Set<String> getUpgradePacks() {
    return upgradePacks;
  }
}
