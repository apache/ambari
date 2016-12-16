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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.ServiceInfo;

public class StackServiceResponse {

  private String stackName;
  private String stackVersion;
  private String serviceName;
  private String serviceType;
  private String serviceDisplayName;
  private String userName;
  private String comments;
  private String serviceVersion;
  private ServiceInfo.Selection selection;
  private boolean serviceCheckSupported;
  private List<String> customCommands;

  private Map<String, Map<String, Map<String, String>>> configTypes;

  private Set<String> excludedConfigTypes;

  private List<String> requiredServices;

  private Map<String, String> serviceProperties;

  /**
   * A File pointing to the service-level Kerberos descriptor file
   *
   * This may be null if a relevant file is not available.
   */
  private File kerberosDescriptorFile;

  /**
   * Indicates if the stack definition says this service supports
   * credential store. If not specified, this will be false.
   */
  private boolean credentialStoreSupported;

  /**
   * Indicates if the stack definition says this service is enabled
   * for credential store use. If not specified, this will be false.
   */
  private boolean credentialStoreEnabled;

  /**
   * Constructor.
   *
   * @param service
   *          the service to generate the response from (not {@code null}).
   */
  public StackServiceResponse(ServiceInfo service) {
    serviceName = service.getName();
    serviceType = service.getServiceType();
    serviceDisplayName = service.getDisplayName();
    userName = null;
    comments = service.getComment();
    serviceVersion = service.getVersion();
    configTypes = service.getConfigTypeAttributes();
    excludedConfigTypes = service.getExcludedConfigTypes();
    requiredServices = service.getRequiredServices();
    serviceCheckSupported = null != service.getCommandScript();
    selection = service.getSelection();

    // the custom command names defined at the service (not component) level
    List<CustomCommandDefinition> definitions = service.getCustomCommands();
    if (null == definitions || definitions.size() == 0) {
      customCommands = Collections.emptyList();
    } else {
      customCommands = new ArrayList<String>(definitions.size());
      for (CustomCommandDefinition command : definitions) {
        customCommands.add(command.getName());
      }
    }

    kerberosDescriptorFile = service.getKerberosDescriptorFile();

    serviceProperties = service.getServiceProperties();

    credentialStoreSupported = service.isCredentialStoreSupported();

    credentialStoreEnabled = service.isCredentialStoreEnabled();
  }

  public ServiceInfo.Selection getSelection() {
    return selection;
  }

  public void setSelection(ServiceInfo.Selection selection) {
    this.selection = selection;
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

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public String getServiceDisplayName() {
    return serviceDisplayName;
  }

  public void setServiceDisplayName(String serviceDisplayName) {
    this.serviceDisplayName = serviceDisplayName;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public String getServiceVersion() {
    return serviceVersion;
  }

  public void setServiceVersion(String serviceVersion) {
    this.serviceVersion = serviceVersion;
  }

  public Map<String, Map<String, Map<String, String>>> getConfigTypes() {
    return configTypes;
  }

  public Set<String> getExcludedConfigTypes() {
    return excludedConfigTypes;
  }

  public List<String> getRequiredServices() {
    return requiredServices;
  }

  public void setRequiredServices(List<String> requiredServices) {
    this.requiredServices = requiredServices;
  }

  /**
   * Gets a File pointing to the service-level Kerberos descriptor
   *
   * @return a File pointing to the service-level Kerberos descriptor, or null if no relevant file is
   * available
   */
  public File getKerberosDescriptorFile() {
    return kerberosDescriptorFile;
  }

  /**
   * Sets the service-level Kerberos descriptor File
   *
   * @param kerberosDescriptorFile a File pointing to the service-level Kerberos descriptor
   */
  public void setKerberosDescriptorFile(File kerberosDescriptorFile) {
    this.kerberosDescriptorFile = kerberosDescriptorFile;
  }

  /**
   * Gets whether the service represented by this response supports running
   * "Service Checks". A service check is possible where there is a custom
   * command defined in the {@code metainfo.xml} of the service definition. This
   * not the same as a custom command defined for a component.
   *
   * @return {@code true} if this service supports running "Service Checks",
   *         {@code false} otherwise.
   *
   */
  public boolean isServiceCheckSupported() {
    return serviceCheckSupported;
  }

  /**
   * Gets the names of all of the custom commands for this service.
   *
   * @return the commands or an empty list (never {@code null}).
   */
  public List<String> getCustomCommands() {
    return customCommands;
  }

  /**
   * Get the service properties of this service.
   * @return the properties or an empty map (never {@code null}).
   */
  public Map<String, String> getServiceProperties() {
    return serviceProperties;
  }

  /**
   * Get whether credential store is supported by the service
   *
   * @return true or false.
   */
  public boolean isCredentialStoreSupported() {
    return credentialStoreSupported;
  }

  /**
   * Set credential store supported value
   *
   * @param credentialStoreSupported
   */
  public void setCredentialStoreSupported(boolean credentialStoreSupported) {
    this.credentialStoreSupported = credentialStoreSupported;
  }

  /**
   * Get whether credential store use is enabled
   *
   * @return true or false
   */
  public boolean isCredentialStoreEnabled() {
    return credentialStoreEnabled;
  }

  /**
   * Set credential store enabled value.
   *
   * @param credentialStoreEnabled
   */
  public void setCredentialStoreEnabled(boolean credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
  }
}
