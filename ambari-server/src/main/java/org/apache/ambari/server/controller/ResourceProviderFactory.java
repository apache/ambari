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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceProvider;

import com.google.inject.name.Named;
import java.util.Set;

public interface ResourceProviderFactory {
  @Named("host")
  ResourceProvider getHostResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController);

  @Named("hostComponent")
  ResourceProvider getHostComponentResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController);

  @Named("service")
  ResourceProvider getServiceResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController);

  @Named("component")
  ResourceProvider getComponentResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController);

  @Named("member")
  ResourceProvider getMemberResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController);

  @Named("hostKerberosIdentity")
  ResourceProvider getHostKerberosIdentityResourceProvider(AmbariManagementController managementController);

  @Named("credential")
  ResourceProvider getCredentialResourceProvider(AmbariManagementController managementController);

  @Named("repositoryVersion")
  ResourceProvider getRepositoryVersionResourceProvider();

  @Named("kerberosDescriptor")
  ResourceProvider getKerberosDescriptorResourceProvider(AmbariManagementController managementController,
                                                         Set<String> propertyIds,
                                                         Map<Resource.Type, String> keyPropertyIds);

}
