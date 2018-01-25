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

/**
 * Encapsulates stack information.
 */
public interface StackInfo {

  Set<StackId> getStacksForService(String serviceName);
  Set<String> getServices(StackId stackId);

  /**
   * Get services contained in the stack.
   *
   * @return collection of all services for the stack
   */
  Collection<String> getServices();

  /**
   * Get components contained in the stack for the specified service.
   *
   * @param service  service name
   *
   * @return collection of component names for the specified service
   */
  Collection<String> getComponents(String service);

  /**
   * Get all service components
   *
   * @return map of service to associated components
   */
  Map<String, Collection<String>> getComponents();

  /**
   * Get info for the specified component.
   *
   * @param component  component name
   *
   * @return component information for the requested component
   *         or null if the component doesn't exist in the stack
   */
  ComponentInfo getComponentInfo(String component);

  /**
   * Get all configuration types, including excluded types for the specified service.
   *
   * @param service  service name
   *
   * @return collection of all configuration types for the specified service
   */
  Collection<String> getAllConfigurationTypes(String service);

  /**
   * Get configuration types for the specified service.
   * This doesn't include any service excluded types.
   *
   * @param service  service name
   * @return collection of all configuration types for the specified service
   */
  Collection<String> getConfigurationTypes(String service);

  /**
   * Get the set of excluded configuration types for this service.
   *
   * @param service service name
   * @return Set of names of excluded config types. Will not return null.
   */
  Set<String> getExcludedConfigurationTypes(String service);

  /**
   * Get config properties for the specified service and configuration type.
   *
   * @param service  service name
   * @param type     configuration type
   * @return map of property names to values for the specified service and configuration type
   */
  Map<String, String> getConfigurationProperties(String service, String type);

  Map<String, Stack.ConfigProperty> getConfigurationPropertiesWithMetadata(String service, String type);

  /**
   * Get all required config properties for the specified service.
   *
   * @param service  service name
   * @return collection of all required properties for the given service
   */
  Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service);

  /**
   * Get required config properties for the specified service which belong to the specified property type.
   *
   * @param service       service name
   * @param propertyType  property type
   *
   * @return collection of required properties for the given service and property type
   */
  Collection<Stack.ConfigProperty> getRequiredConfigurationProperties(String service, PropertyInfo.PropertyType propertyType);

  boolean isPasswordProperty(String service, String type, String propertyName);

  Map<String, String> getStackConfigurationProperties(String type);

  boolean isKerberosPrincipalNameProperty(String service, String type, String propertyName);

  /**
   * Get config attributes for the specified service and configuration type.
   *
   * @param service  service name
   * @param type     configuration type
   *
   * @return  map of attribute names to map of property names to attribute values
   *          for the specified service and configuration type
   */
  Map<String, Map<String, String>> getConfigurationAttributes(String service, String type);

  Map<String, Map<String, String>> getStackConfigurationAttributes(String type);

  /**
   * Get the service for the specified component.
   *
   * @param component  component name
   *
   * @return service name that contains tha specified component
   */
  String getServiceForComponent(String component);

  /**
   * Get the names of the services which contains the specified components.
   *
   * @param components collection of components
   *
   * @return collection of services which contain the specified components
   */
  Collection<String> getServicesForComponents(Collection<String> components);

  /**
   * Obtain the service name which corresponds to the specified configuration.
   *
   * @param config  configuration type
   *
   * @return name of service which corresponds to the specified configuration type
   */
  String getServiceForConfigType(String config);

  Stream<String> getServicesForConfigType(String config);

  /**
   * Return the dependencies specified for the given component.
   *
   * @param component  component to get dependency information for
   *
   * @return collection of dependency information for the specified component
   */
  //todo: full dependency graph
  Collection<DependencyInfo> getDependenciesForComponent(String component);

  /**
   * Get the service, if any, that a component dependency is conditional on.
   *
   * @param dependency  dependency to get conditional service for
   *
   * @return conditional service for provided component or null if dependency
   *         is not conditional on a service
   */
  String getConditionalServiceForDependency(DependencyInfo dependency);

  String getExternalComponentConfig(String component);

  /**
   * Obtain the required cardinality for the specified component.
   */
  Cardinality getCardinality(String component);

  /**
   * Obtain auto-deploy information for the specified component.
   */
  AutoDeployInfo getAutoDeployInfo(String component);

  boolean isMasterComponent(String component);

  Configuration getConfiguration(Collection<String> services);

  Configuration getConfiguration();
}
