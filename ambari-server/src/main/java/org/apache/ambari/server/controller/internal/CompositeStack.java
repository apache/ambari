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

// FIXME temporary
/** Combine multiple mpacks into a single stack. */
public class CompositeStack implements StackInfo {

  private final Set<Stack> mpacks;
  private final Collection<String> services;
  private final Map<String, Collection<String>> components;

  public CompositeStack(Set<Stack> mpacks) {
    this.mpacks = mpacks;

    services = mpacks.stream()
      .flatMap(s -> s.getServices().stream())
      .collect(toSet());

    components = mpacks.stream()
      .flatMap(m -> m.getComponents().entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Set<StackId> getStacksForService(String serviceName) {
    return mpacks.stream()
      .map(m -> Pair.of(m.getStackId(), m.getServices()))
      .filter(p -> p.getRight().contains(serviceName))
      .map(Pair::getLeft)
      .collect(toSet());
  }

  public Set<String> getServices(StackId stackId) {
    return mpacks.stream()
      .filter(m -> stackId.equals(m.getStackId()))
      .findAny()
      .flatMap(m -> Optional.of(ImmutableSet.copyOf(m.getServices())))
      .orElse(ImmutableSet.of());
  }

  @Override
  public Collection<String> getServices() {
    return services;
  }

  @Override
  public Collection<String> getComponents(String service) {
    return components.get(service);
  }

  @Override
  public Map<String, Collection<String>> getComponents() {
    return components;
  }

  @Override
  public ComponentInfo getComponentInfo(String component) {
    return mpacks.stream()
      .map(m -> m.getComponentInfo(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public Collection<String> getAllConfigurationTypes(String service) {
    return mpacks.stream()
      .flatMap(m -> m.getAllConfigurationTypes(service).stream())
      .collect(toSet());
  }

  @Override
  public Collection<String> getConfigurationTypes(String service) {
    return mpacks.stream()
      .flatMap(m -> m.getConfigurationTypes(service).stream())
      .collect(toSet());
  }

  @Override
  public Set<String> getExcludedConfigurationTypes(String service) {
    return mpacks.stream()
      .flatMap(m -> m.getExcludedConfigurationTypes(service).stream())
      .collect(toSet());
  }

  @Override
  public Map<String, String> getConfigurationProperties(String service, String type) {
    return mpacks.stream()
      .flatMap(m -> m.getConfigurationProperties(service, type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public Map<String, Stack.ConfigProperty> getConfigurationPropertiesWithMetadata(String service, String type) {
    return mpacks.stream()
      .flatMap(m -> m.getConfigurationPropertiesWithMetadata(service, type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service) {
    return mpacks.stream()
      .flatMap(m -> m.getRequiredConfigurationProperties(service).stream())
      .collect(toSet());
  }

  @Override
  public Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service, PropertyInfo.PropertyType propertyType) {
    return mpacks.stream()
      .flatMap(m -> m.getRequiredConfigurationProperties(service, propertyType).stream())
      .collect(toSet());
  }

  @Override
  public boolean isPasswordProperty(String service, String type, String propertyName) {
    return mpacks.stream()
      .anyMatch(s -> s.isPasswordProperty(service, type, propertyName));
  }

  @Override
  public Map<String, String> getStackConfigurationProperties(String type) {
    return mpacks.stream()
      .flatMap(m -> m.getStackConfigurationProperties(type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public boolean isKerberosPrincipalNameProperty(String service, String type, String propertyName) {
    return mpacks.stream()
      .anyMatch(s -> s.isKerberosPrincipalNameProperty(service, type, propertyName));
  }

  @Override
  public Map<String, Map<String, String>> getConfigurationAttributes(String service, String type) {
    return mpacks.stream()
      .flatMap(m -> m.getConfigurationAttributes(service, type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public Map<String, Map<String, String>> getStackConfigurationAttributes(String type) {
    return mpacks.stream()
      .flatMap(m -> m.getStackConfigurationAttributes(type).entrySet().stream())
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public String getServiceForComponent(String component) {
    return mpacks.stream()
      .map(m -> m.getServiceForComponent(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public Collection<String> getServicesForComponents(Collection<String> components) {
    return mpacks.stream()
      .flatMap(m -> m.getServicesForComponents(components).stream())
      .collect(toSet());
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
    return mpacks.stream()
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
    return mpacks.stream()
      .flatMap(m -> m.getDependenciesForComponent(component).stream())
      .collect(toSet());
  }

  @Override
  public String getConditionalServiceForDependency(DependencyInfo dependency) {
    return mpacks.stream()
      .map(m -> m.getConditionalServiceForDependency(dependency))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public String getExternalComponentConfig(String component) {
    return mpacks.stream()
      .map(m -> m.getExternalComponentConfig(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public Cardinality getCardinality(String component) {
    return mpacks.stream()
      .map(m -> m.getCardinality(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public AutoDeployInfo getAutoDeployInfo(String component) {
    return mpacks.stream()
      .map(m -> m.getAutoDeployInfo(component))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public boolean isMasterComponent(String component) {
    return mpacks.stream()
      .anyMatch(s -> s.isMasterComponent(component));
  }

  @Override
  public Configuration getConfiguration(Collection<String> services) {
    // FIXME probably too costly
    return mpacks.stream()
      .map(m -> m.getConfiguration(services))
      .reduce(Configuration.createEmpty(), Configuration::combine);
  }

  @Override
  public Configuration getConfiguration() {
    // FIXME probably too costly
    return mpacks.stream()
      .map(StackInfo::getConfiguration)
      .reduce(Configuration.createEmpty(), Configuration::combine);
  }
}
