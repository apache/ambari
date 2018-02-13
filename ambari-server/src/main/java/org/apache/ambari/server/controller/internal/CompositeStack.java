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
package org.apache.ambari.server.controller.internal;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.Configuration;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableSet;

/** Combines multiple mpacks into a single stack. */
// TODO move to topology package
public class CompositeStack implements StackDefinition {

  private final Set<Stack> stacks;

  CompositeStack(Set<Stack> stacks) {
    this.stacks = stacks;
  }

  @Override
  public Set<StackId> getStacksForService(String serviceName) {
    return stacks.stream()
      .map(m -> Pair.of(m.getStackId(), m.getServices()))
      .filter(p -> p.getRight().contains(serviceName))
      .map(Pair::getLeft)
      .collect(toSet());
  }

  @Override
  public Set<StackId> getStacksForComponent(String componentName) {
    return stacks.stream()
      .map(m -> m.getStacksForComponent(componentName))
      .filter(s -> !s.isEmpty())
      .flatMap(Collection::stream)
      .collect(toSet());
  }

  @Override
  public Set<String> getServices(StackId stackId) {
    return stacks.stream()
      .filter(m -> stackId.equals(m.getStackId()))
      .findAny()
      .flatMap(m -> Optional.of(ImmutableSet.copyOf(m.getServices())))
      .orElse(ImmutableSet.of());
  }

  @Override
  public Set<StackId> getStackIds() {
    return stacks.stream()
      .map(Stack::getStackId)
      .collect(toSet());
  }

  @Override
  public Collection<String> getServices() {
    return stacks.stream()
      .flatMap(s -> s.getServices().stream())
      .collect(toSet());
  }

  @Override
  public Collection<String> getComponents(String service) {
    return stacks.stream()
      .map(s -> s.getComponents(service))
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .collect(toSet());
  }

  @Override
  public Collection<String> getComponents() {
    return stacks.stream()
      .flatMap(s -> s.getComponents().stream())
      .collect(toSet());
  }

  @Override
  public ComponentInfo getComponentInfo(String component) {
    return stacks.stream()
      .map(m -> m.getComponentInfo(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public Collection<String> getAllConfigurationTypes(String service) {
    return stacks.stream()
      .flatMap(m -> m.getAllConfigurationTypes(service).stream())
      .collect(toSet());
  }

  @Override
  public Collection<String> getConfigurationTypes(String service) {
    return stacks.stream()
      .flatMap(m -> m.getConfigurationTypes(service).stream())
      .collect(toSet());
  }

  @Override
  public Set<String> getExcludedConfigurationTypes(String service) {
    return stacks.stream()
      .flatMap(m -> m.getExcludedConfigurationTypes(service).stream())
      .collect(toSet());
  }

  @Override
  public Map<String, String> getConfigurationProperties(String service, String type) {
    return stacks.stream()
      .flatMap(m -> m.getConfigurationProperties(service, type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public Map<String, Stack.ConfigProperty> getConfigurationPropertiesWithMetadata(String service, String type) {
    return stacks.stream()
      .flatMap(m -> m.getConfigurationPropertiesWithMetadata(service, type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service) {
    return stacks.stream()
      .flatMap(m -> m.getRequiredConfigurationProperties(service).stream())
      .collect(toSet());
  }

  @Override
  public Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service, PropertyInfo.PropertyType propertyType) {
    return stacks.stream()
      .flatMap(m -> m.getRequiredConfigurationProperties(service, propertyType).stream())
      .collect(toSet());
  }

  @Override
  public boolean isPasswordProperty(String service, String type, String propertyName) {
    return stacks.stream()
      .anyMatch(s -> s.isPasswordProperty(service, type, propertyName));
  }

  @Override
  public Map<String, String> getStackConfigurationProperties(String type) {
    return stacks.stream()
      .flatMap(m -> m.getStackConfigurationProperties(type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public boolean isKerberosPrincipalNameProperty(String service, String type, String propertyName) {
    return stacks.stream()
      .anyMatch(s -> s.isKerberosPrincipalNameProperty(service, type, propertyName));
  }

  @Override
  public Map<String, Map<String, String>> getConfigurationAttributes(String service, String type) {
    return stacks.stream()
      .flatMap(m -> m.getConfigurationAttributes(service, type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public Map<String, Map<String, String>> getStackConfigurationAttributes(String type) {
    return stacks.stream()
      .flatMap(m -> m.getStackConfigurationAttributes(type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public String getServiceForComponent(String component) {
    return stacks.stream()
      .map(m -> m.getServiceForComponent(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  @Nonnull
  public Stream<Pair<StackId, String>> getServicesForComponent(String component) {
    return stacks.stream()
      .flatMap(stack -> stack.getServicesForComponent(component));
  }

  @Override
  public String getServiceForConfigType(String config) {
    if (ConfigHelper.CLUSTER_ENV.equals(config)) { // for backwards compatibility
      return null;
    }
    return getServicesForConfigType(config)
      .findAny()
      .orElseThrow(() -> new IllegalArgumentException(Stack.formatMissingServiceForConfigType(config, "ANY")));
  }

  @Override
  public Stream<String> getServicesForConfigType(String config) {
    if (ConfigHelper.CLUSTER_ENV.equals(config)) { // for backwards compatibility
      return Stream.empty();
    }
    return stacks.stream()
      .map(m -> {
        try {
          return m.getServiceForConfigType(config);
        } catch (IllegalArgumentException e) {
          return null;
        }
      })
      .filter(Objects::nonNull);
  }

  @Override
  public Collection<DependencyInfo> getDependenciesForComponent(String component) {
    return stacks.stream()
      .flatMap(m -> m.getDependenciesForComponent(component).stream())
      .collect(toSet());
  }

  @Override
  public String getExternalComponentConfig(String component) {
    return stacks.stream()
      .map(m -> m.getExternalComponentConfig(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public Cardinality getCardinality(String component) {
    return stacks.stream()
      .map(m -> m.getCardinality(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public AutoDeployInfo getAutoDeployInfo(String component) {
    return stacks.stream()
      .map(m -> m.getAutoDeployInfo(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public boolean isMasterComponent(String component) {
    return stacks.stream()
      .anyMatch(s -> s.isMasterComponent(component));
  }

  @Override
  public Configuration getConfiguration(Collection<String> services) {
    // FIXME probably too costly
    return stacks.stream()
      .map(m -> m.getConfiguration(services))
      .reduce(Configuration.createEmpty(), Configuration::combine);
  }

  @Override
  public Configuration getConfiguration() {
    // FIXME probably too costly
    return stacks.stream()
      .map(StackDefinition::getConfiguration)
      .reduce(Configuration.createEmpty(), Configuration::combine);
  }
}
