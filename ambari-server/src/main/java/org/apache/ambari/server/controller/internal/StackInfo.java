package org.apache.ambari.server.controller.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.Configuration;

// FIXME move javadoc
public interface StackInfo {

  Set<StackId> getStacksForService(String serviceName);
  Set<String> getServices(StackId stackId);

  Collection<String> getServices();

  Collection<String> getComponents(String service);

  Map<String, Collection<String>> getComponents();

  ComponentInfo getComponentInfo(String component);

  Collection<String> getAllConfigurationTypes(String service);

  Collection<String> getConfigurationTypes(String service);

  Set<String> getExcludedConfigurationTypes(String service);

  Map<String, String> getConfigurationProperties(String service, String type);

  Map<String, Stack.ConfigProperty> getConfigurationPropertiesWithMetadata(String service, String type);

  Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service);

  Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service, PropertyInfo.PropertyType propertyType);

  boolean isPasswordProperty(String service, String type, String propertyName);

  Map<String, String> getStackConfigurationProperties(String type);

  boolean isKerberosPrincipalNameProperty(String service, String type, String propertyName);

  Map<String, Map<String, String>> getConfigurationAttributes(String service, String type);

  Map<String, Map<String, String>> getStackConfigurationAttributes(String type);

  String getServiceForComponent(String component);

  Collection<String> getServicesForComponents(Collection<String> components);

  String getServiceForConfigType(String config);
  Stream<String> getServicesForConfigType(String config);

  Collection<DependencyInfo> getDependenciesForComponent(String component);

  String getConditionalServiceForDependency(DependencyInfo dependency);

  String getExternalComponentConfig(String component);

  Cardinality getCardinality(String component);

  AutoDeployInfo getAutoDeployInfo(String component);

  boolean isMasterComponent(String component);

  Configuration getConfiguration(Collection<String> services);

  Configuration getConfiguration();
}
