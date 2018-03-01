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

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * I am the Blueprint and ProvisionClusterRequest combined.
 */
public class BlueprintBasedClusterProvisionRequest implements Blueprint, ProvisionRequest {

  private static final Logger LOG = LoggerFactory.getLogger(BlueprintBasedClusterProvisionRequest.class);

  private final Blueprint blueprint;
  private final ProvisionClusterRequest request;
  private final Set<StackId> stackIds;
  private final StackDefinition stack;
  private final Map<String, MpackInstance> mpacks;
  private final SecurityConfiguration securityConfiguration;

  public BlueprintBasedClusterProvisionRequest(AmbariContext ambariContext, SecurityConfigurationFactory securityConfigurationFactory, Blueprint blueprint, ProvisionClusterRequest request) {
    this.blueprint = blueprint;
    this.request = request;

    stackIds = ImmutableSet.copyOf(Sets.union(blueprint.getStackIds(), request.getStackIds()));
    stack = ambariContext.composeStacks(stackIds);
    mpacks = ImmutableMap.copyOf(
      Stream.concat(blueprint.getMpacks().stream(), request.getMpacks().stream())
        .collect(toMap(MpackInstance::getMpackName, Function.identity())));

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
    return mpacks.values();
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
  public BlueprintEntity toEntity() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Long getClusterId() {
    return null;
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

  public String getDefaultPassword() {
    return request.getDefaultPassword();
  }

  public ConfigRecommendationStrategy getConfigRecommendationStrategy() {
    return request.getConfigRecommendationStrategy();
  }

  public ProvisionAction getProvisionAction() {
    return request.getProvisionAction();
  }

  public StackDefinition getStack() {
    return stack;
  }

  public Map<String, Map<String, ServiceInstance>> getServicesByMpack() {
    Map<String, Map<String, ServiceInstance>> result = new HashMap<>();
    for (MpackInstance mpack : mpacks.values()) {
      Map<String, ServiceInstance> services = mpack.getServiceInstances().stream()
        .collect(toMap(ServiceInstance::getName, Function.identity()));
      result.put(mpack.getMpackName(), services);
    }
    return result;
  }

  /**
   * @return service instances defined in the topology, mapped by service name,
   * whose name is unique across all mpacks.
   */
  public Map<String, ServiceInstance> getUniqueServices() {
    Map<String, ServiceInstance> map = mpacks.values().stream()
      .flatMap(mpack -> mpack.getServiceInstances().stream())
      .collect(toMap(ServiceInstance::getName, Function.identity(), (s1, s2) -> null));
    map.entrySet().removeIf(e -> e.getValue() == null); // remove non-unique names mapped to null
    return map;
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
}
