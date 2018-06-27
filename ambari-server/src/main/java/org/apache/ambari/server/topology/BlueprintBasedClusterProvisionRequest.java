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
package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * I am the Blueprint and ProvisionRequest combined.
 */
public class BlueprintBasedClusterProvisionRequest implements Blueprint, ProvisionRequest {

  private static final Logger LOG = LoggerFactory.getLogger(BlueprintBasedClusterProvisionRequest.class);

  private final Blueprint blueprint;
  private final ProvisionRequest request;
  private final Set<StackId> stackIds;
  private final StackDefinition stack;
  private final Set<MpackInstance> mpacks;
  private final SecurityConfiguration securityConfiguration;

  public BlueprintBasedClusterProvisionRequest(AmbariContext ambariContext, SecurityConfigurationFactory securityConfigurationFactory, Blueprint blueprint, ProvisionRequest request) {
    this.blueprint = blueprint;
    this.request = request;
    stackIds = ImmutableSet.copyOf(Sets.union(blueprint.getStackIds(), request.getStackIds()));
    mpacks = ImmutableSet.<MpackInstance>builder().
      addAll(blueprint.getMpacks()).
      addAll(request.getMpacks()).build();

    stack = ambariContext.composeStacks(stackIds);

    securityConfiguration = processSecurityConfiguration(securityConfigurationFactory);

    if (securityConfiguration.getType() == SecurityType.KERBEROS) {
      ensureKerberosClientIsPresent();
    }
  }

  @Override
  public String getName() {
    return blueprint.getName();
  }

  @Override
  public Map<String, HostGroup> getHostGroups() {
    return blueprint.getHostGroups();
  }

  @Override
  public HostGroup getHostGroup(String name) {
    return blueprint.getHostGroup(name);
  }

  @Override
  public Configuration getConfiguration() {
    return request.getConfiguration();
  }

  @Override
  public Setting getSetting() {
    return blueprint.getSetting();
  }

  @Override
  public Set<StackId> getStackIds() {
    return stackIds;
  }

  @Override
  public Collection<MpackInstance> getMpacks() {
    return mpacks;
  }

  @Override
  public Collection<HostGroup> getHostGroupsForComponent(String component) {
    return blueprint.getHostGroupsForComponent(component);
  }

  @Nonnull
  @Override
  public SecurityConfiguration getSecurity() {
    return securityConfiguration;
  }

  @Override
  public SecurityConfiguration getSecurityConfiguration() {
    return securityConfiguration;
  }

  @Override
  public BlueprintEntity toEntity() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Long getClusterId() {
    return request.getClusterId();
  }

  @Override
  public Type getType() {
    return Type.PROVISION;
  }

  @Override
  public Blueprint getBlueprint() {
    return blueprint;
  }

  @Override
  public Map<String, HostGroupInfo> getHostGroupInfo() {
    return request.getHostGroupInfo();
  }

  @Override
  public String getDescription() {
    return request.getDescription();
  }

  @Override
  public String getDefaultPassword() {
    return request.getDefaultPassword();
  }

  @Override
  public ConfigRecommendationStrategy getConfigRecommendationStrategy() {
    return request.getConfigRecommendationStrategy();
  }

  @Override
  public ProvisionAction getProvisionAction() {
    return request.getProvisionAction();
  }

  public StackDefinition getStack() {
    return stack;
  }

  /**
   * Retrieve security info from Blueprint if missing from Cluster Template request.
   */
  private SecurityConfiguration processSecurityConfiguration(SecurityConfigurationFactory securityConfigurationFactory) {
    SecurityConfiguration blueprintSecurity = blueprint.getSecurity();
    SecurityConfiguration requestSecurity = request.getSecurityConfiguration();

    if (requestSecurity == null) {
      LOG.debug("There's no security configuration in the request, retrieving it from the associated blueprint");
      requestSecurity = blueprintSecurity;
      if (requestSecurity.getType() == SecurityType.KERBEROS && requestSecurity.getDescriptorReference() != null) {
        requestSecurity = securityConfigurationFactory.loadSecurityConfigurationByReference(requestSecurity.getDescriptorReference());
      }
    } else if (requestSecurity.getType() == SecurityType.NONE && blueprintSecurity.getType() == SecurityType.KERBEROS) {
      throw new IllegalArgumentException("Setting security to NONE is not allowed as security type in blueprint is set to KERBEROS!");
    }

    return requestSecurity;
  }

  @Override
  public boolean ensureKerberosClientIsPresent() {
    return blueprint.ensureKerberosClientIsPresent();
  }

  public Set<MpackInstance> getAllMpacks() {
    return mpacks;
  }

}
