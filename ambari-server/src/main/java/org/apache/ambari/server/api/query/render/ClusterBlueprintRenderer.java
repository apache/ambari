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

package org.apache.ambari.server.api.query.render;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.services.ResultPostProcessorImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.BlueprintConfigurationProcessor;
import org.apache.ambari.server.controller.internal.HostGroup;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.PropertyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renderer which renders a cluster resource as a blueprint.
 */
public class ClusterBlueprintRenderer extends BaseRenderer implements Renderer {

  /**
   * Management Controller used to get stack information.
   */
  private AmbariManagementController controller = AmbariServer.getController();

  /**
   * Map of configuration type to configuration properties which are required that a user
   * input.  These properties will be stripped from the exported blueprint.
   */
  private Map<String, Collection<String>> propertiesToStrip = new HashMap<String, Collection<String>>();

  private final static Logger LOG = LoggerFactory.getLogger(ClusterBlueprintRenderer.class);


  // ----- Renderer ----------------------------------------------------------

  @Override
  public TreeNode<Set<String>> finalizeProperties(
      TreeNode<QueryInfo> queryProperties, boolean isCollection) {

    Set<String> properties = new HashSet<String>(queryProperties.getObject().getProperties());
    TreeNode<Set<String>> resultTree = new TreeNodeImpl<Set<String>>(
        null, properties, queryProperties.getName());

    copyPropertiesToResult(queryProperties, resultTree);

    String configType = Resource.Type.Configuration.name();
    if (resultTree.getChild(configType) == null) {
      resultTree.addChild(new HashSet<String>(), configType);
    }

    String serviceType = Resource.Type.Service.name();
    if (resultTree.getChild(serviceType) == null) {
      resultTree.addChild(new HashSet<String>(), serviceType);
    }

    String hostType = Resource.Type.Host.name();
    String hostComponentType = Resource.Type.HostComponent.name();
    TreeNode<Set<String>> hostComponentNode = resultTree.getChild(
        hostType + "/" + hostComponentType);

    if (hostComponentNode == null) {
      TreeNode<Set<String>> hostNode = resultTree.getChild(hostType);
      if (hostNode == null) {
        hostNode = resultTree.addChild(new HashSet<String>(), hostType);
      }
      hostComponentNode = hostNode.addChild(new HashSet<String>(), hostComponentType);
    }
    resultTree.getChild(configType).getObject().add("properties");
    hostComponentNode.getObject().add("HostRoles/component_name");

    return resultTree;
  }

  @Override
  public Result finalizeResult(Result queryResult) {
    TreeNode<Resource> resultTree = queryResult.getResultTree();
    Result result = new ResultImpl(true);
    TreeNode<Resource> blueprintResultTree = result.getResultTree();
    if (isCollection(resultTree)) {
      blueprintResultTree.setProperty("isCollection", "true");
    }

    for (TreeNode<Resource> node : resultTree.getChildren()) {
      Resource blueprintResource = createBlueprintResource(node);
      blueprintResultTree.addChild(new TreeNodeImpl<Resource>(
          blueprintResultTree, blueprintResource, node.getName()));
    }
    return result;
  }

  @Override
  public ResultPostProcessor getResultPostProcessor(Request request) {
    return new BlueprintPostProcessor(request);
  }

  // ----- private instance methods ------------------------------------------

  /**
   * Create a blueprint resource.
   *
   * @param clusterNode  cluster tree node
   *
   * @return a new blueprint resource
   */
  private Resource createBlueprintResource(TreeNode<Resource> clusterNode) {
    Resource clusterResource = clusterNode.getObject();
    Resource blueprintResource = new ResourceImpl(Resource.Type.Cluster);

    String[] stackTokens = ((String) clusterResource.getPropertyValue(
            PropertyHelper.getPropertyId("Clusters", "version"))).split("-");

    blueprintResource.setProperty("Blueprints/stack_name", stackTokens[0]);
    blueprintResource.setProperty("Blueprints/stack_version", stackTokens[1]);

    Collection<HostGroupImpl> hostGroups =  processHostGroups(clusterNode.getChild("hosts"));

    List<Map<String, Object>> groupList = formatGroupsAsList(hostGroups);
    blueprintResource.setProperty("host_groups", groupList);

    determinePropertiesToStrip(clusterNode.getChild("services"), stackTokens[0], stackTokens[1]);

    blueprintResource.setProperty("configurations", processConfigurations(clusterNode, hostGroups));

    return blueprintResource;
  }

  /**
   * Determine which configuration properties need to be stripped from the configuration prior to exporting.
   * Stripped properties are any property which are marked as required in the stack definition.  For example,
   * all passwords are required properties and are therefore not exported.
   *
   * @param servicesNode  services node
   * @param stackName     stack name
   * @param stackVersion  stack version
   */
  private void determinePropertiesToStrip(TreeNode<Resource> servicesNode, String stackName, String stackVersion) {
    AmbariMetaInfo stackInfo = getController().getAmbariMetaInfo();
    for (TreeNode<Resource> service : servicesNode.getChildren()) {
      String name = (String) service.getObject().getPropertyValue("ServiceInfo/service_name");
      Map<String, PropertyInfo> requiredProperties = stackInfo.getRequiredProperties(stackName, stackVersion, name);
      for (Map.Entry<String, PropertyInfo> entry : requiredProperties.entrySet()) {
        String propertyName = entry.getKey();
        PropertyInfo propertyInfo = entry.getValue();
        String configCategory = propertyInfo.getFilename();
        if (configCategory.endsWith(".xml")) {
          configCategory = configCategory.substring(0, configCategory.indexOf(".xml"));
        }
        Collection<String> categoryProperties = propertiesToStrip.get(configCategory);
        if (categoryProperties == null) {
          categoryProperties = new ArrayList<String>();
          propertiesToStrip.put(configCategory, categoryProperties);
        }
        categoryProperties.add(propertyName);
      }
    }
  }

  /**
   * Process cluster scoped configurations.
   *
   * @param clusterNode  cluster node
   * @param hostGroups   all host groups
   *
   * @return cluster configuration
   */
  private List<Map<String, Map<String, String>>>  processConfigurations(TreeNode<Resource> clusterNode,
                                                                        Collection<HostGroupImpl> hostGroups) {

    List<Map<String, Map<String, String>>> configList = new ArrayList<Map<String, Map<String, String>>>();

    Map<String, Object> desiredConfigMap = clusterNode.getObject().getPropertiesMap().get("Clusters/desired_configs");
    TreeNode<Resource> configNode = clusterNode.getChild("configurations");
    for (TreeNode<Resource> config : configNode.getChildren()) {
      Configuration configuration = new Configuration(config);
      DesiredConfig desiredConfig = (DesiredConfig) desiredConfigMap.get(configuration.getType());
      if (desiredConfig != null && desiredConfig.getTag().equals(configuration.getTag())) {
        Map<String, Map<String, String>> properties = Collections.singletonMap(
            configuration.getType(), configuration.getProperties());

        BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
        properties = updater.doUpdateForBlueprintExport(hostGroups);
        configList.add(properties);
      }
    }
    return configList;
  }

  /**
   * Process cluster host groups.
   *
   * @param hostNode  host node
   *
   * @return collection of host groups
   */
  private Collection<HostGroupImpl> processHostGroups(TreeNode<Resource> hostNode) {
    Map<HostGroupImpl, HostGroupImpl> mapHostGroups = new HashMap<HostGroupImpl, HostGroupImpl>();
    int count = 1;
    for (TreeNode<Resource> host : hostNode.getChildren()) {
      HostGroupImpl group = new HostGroupImpl(host);
      String hostName = (String) host.getObject().getPropertyValue(
          PropertyHelper.getPropertyId("Hosts", "host_name"));

      if (mapHostGroups.containsKey(group)) {
        HostGroupImpl hostGroup = mapHostGroups.get(group);
        hostGroup.incrementCardinality();
        hostGroup.addHost(hostName);
      } else {
        mapHostGroups.put(group, group);
        group.setName("host_group_" + count++);
        group.addHost(hostName);
      }
    }
    return mapHostGroups.values();
  }


  /**
   * Process host group information for all hosts.
   *
   * @param hostGroups all host groups
   *
   * @return list of host group property maps, one element for each host group
   */
  private List<Map<String, Object>> formatGroupsAsList(Collection<HostGroupImpl> hostGroups) {
    List<Map<String, Object>> listHostGroups = new ArrayList<Map<String, Object>>();
    for (HostGroupImpl group : hostGroups) {
      Map<String, Object> mapGroupProperties = new HashMap<String, Object>();
      listHostGroups.add(mapGroupProperties);

      mapGroupProperties.put("name", group.getName());
      mapGroupProperties.put("cardinality", String.valueOf(group.getCardinality()));
      mapGroupProperties.put("components", processHostGroupComponents(group));
      List<Map<String, Map<String, String>>> hostConfigurations = new ArrayList<Map<String, Map<String, String>>>();
      for (Configuration configuration : group.getConfigurations()) {
        Map<String, Map<String, String>> propertyMap = Collections.singletonMap(
            configuration.getType(), configuration.properties);
        BlueprintConfigurationProcessor configurationProcessor = new BlueprintConfigurationProcessor(propertyMap);
        Map<String, Map<String, String>> updatedProps = configurationProcessor.doUpdateForBlueprintExport(hostGroups);
        hostConfigurations.add(updatedProps);

      }
      mapGroupProperties.put("configurations", hostConfigurations);
    }
    return listHostGroups;
  }

  /**
   * Process host group component information for a specific host.
   *
   * @param group host group instance
   *
   * @return list of component names for the host
   */
  private List<Map<String, String>> processHostGroupComponents(HostGroupImpl group) {
    List<Map<String, String>> listHostGroupComponents = new ArrayList<Map<String, String>>();
    for (String component : group.getComponents()) {
      Map<String, String> mapComponentProperties = new HashMap<String, String>();
      listHostGroupComponents.add(mapComponentProperties);
      mapComponentProperties.put("name", component);
    }
    return listHostGroupComponents;
  }

  /**
   * Determine whether a node represents a collection.
   *
   * @param node  node which is evaluated for being a collection
   *
   * @return true if the node represents a collection; false otherwise
   */
  private boolean isCollection(TreeNode<Resource> node) {
    String isCollection = node.getProperty("isCollection");
    return isCollection != null && isCollection.equals("true");
  }

  /**
   * Get management controller instance.
   *
   * @return  management controller
   */
  protected AmbariManagementController getController() {
    return controller;
  }

  // ----- Host Group inner class --------------------------------------------

  /**
   * Host Group representation.
   */
  private class HostGroupImpl implements HostGroup {

    /**
     * Host Group name.
     *
     */
    private String name;

    /**
     * Associated components.
     */
    private Set<String> components = new HashSet<String>();

    /**
     * Host group scoped configurations.
     */
    private Collection<Configuration> configurations = new HashSet<Configuration>();

    /**
     * Number of instances.
     */
    private int m_cardinality = 1;

    /**
     * Collection of associated hosts.
     */
    private Collection<String> hosts = new HashSet<String>();

    /**
     * Constructor.
     *
     * @param host  host node
     */
    public HostGroupImpl(TreeNode<Resource> host) {
      TreeNode<Resource> components = host.getChild("host_components");
      for (TreeNode<Resource> component : components.getChildren()) {
        getComponents().add((String) component.getObject().getPropertyValue(
            "HostRoles/component_name"));
      }
      addAmbariComponentIfLocalhost((String) host.getObject().getPropertyValue(
          PropertyHelper.getPropertyId("Hosts", "host_name")));

      processGroupConfiguration(host);
    }

    /**
     * Preocess host group configuration.
     *
     * @param host  host node
     */
    private void processGroupConfiguration(TreeNode<Resource> host) {
      Map<String, Object> desiredConfigMap = host.getObject().getPropertiesMap().get("Hosts/desired_configs");
      if (desiredConfigMap != null) {
        for (Map.Entry<String, Object> entry : desiredConfigMap.entrySet()) {
          String type = entry.getKey();
          HostConfig hostConfig = (HostConfig) entry.getValue();
          Map<Long, String> overrides = hostConfig.getConfigGroupOverrides();

          if (overrides != null && ! overrides.isEmpty()) {
            Long version = Collections.max(overrides.keySet());
            String tag = overrides.get(version);
            TreeNode<Resource> clusterNode = host.getParent().getParent();
            TreeNode<Resource> configNode = clusterNode.getChild("configurations");
            for (TreeNode<Resource> config : configNode.getChildren()) {
              Configuration configuration = new Configuration(config);
              if (type.equals(configuration.getType()) && tag.equals(configuration.getTag())) {
                getConfigurations().add(configuration);
                break;
              }
            }
          }
        }
      }
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Set<String> getComponents() {
      return components;
    }

    @Override
    public Collection<String> getHostInfo() {
      return hosts;
    }

    @Override
    public Map<String, Map<String, String>> getConfigurationProperties() {
      Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
      for (Configuration configuration : configurations) {
        properties.put(configuration.getType(), configuration.getProperties());
      }

      return properties;
    }

    /**
     * Set the name.
     *
     * @param  name name of host group
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Add a host.
     *
     * @param host  host to add
     */
    public void addHost(String host) {
      hosts.add(host);
    }

    /**
     * Obtain associated host group scoped configurations.
     *
     * @return collection of host group scoped configurations
     */
    public Collection<Configuration> getConfigurations() {
      return configurations;
    }

    /**
     * Obtain the number of instances associated with this host group.
     *
     * @return number of hosts associated with this host group
     */
    public int getCardinality() {
      return m_cardinality;
    }

    /**
     * Increment the cardinality count by one.
     */
    public void incrementCardinality() {
      m_cardinality += 1;
    }

    /**
     * Add the AMBARI_SERVER component if the host is the local host.
     *
     * @param hostname  host to check
     */
    private void addAmbariComponentIfLocalhost(String hostname) {
      try {
        InetAddress hostAddress = InetAddress.getByName(hostname);
        try {
          if (hostAddress.equals(InetAddress.getLocalHost())) {
            getComponents().add("AMBARI_SERVER");
          }
        } catch (UnknownHostException e) {
          //todo: SystemException?
          throw new RuntimeException("Unable to obtain local host name", e);
        }
      } catch (UnknownHostException e) {
        // ignore
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      HostGroupImpl hostGroup = (HostGroupImpl) o;

      return components.equals(hostGroup.components) &&
          configurations.equals(hostGroup.configurations);
    }

    @Override
    public int hashCode() {
      int result = components.hashCode();
      result = 31 * result + configurations.hashCode();
      return result;
    }
  }

  /**
   * Encapsulates a configuration.
   */
  private class Configuration {
    /**
     * Configuration type such as hdfs-site.
     */
    private String type;

    /**
     * Configuration tag.
     */
    private String tag;

    /**
     * Properties of the configuration.
     */
    private Map<String, String> properties = new HashMap<String, String>();

    /**
     * Constructor.
     *
     * @param configNode  configuration node
     */
    @SuppressWarnings("unchecked")
    public Configuration(TreeNode<Resource> configNode) {
      Resource configResource = configNode.getObject();
      type = (String) configResource.getPropertyValue("type");
      tag  = (String) configResource.getPropertyValue("tag");

      // property map type is currently <String, Object>
      properties = (Map) configNode.getObject().getPropertiesMap().get("properties");

      if (properties != null) {
        stripRequiredProperties(properties);
      } else {
        LOG.warn("Empty configuration found for configuration type = " + type +
          " during Blueprint export.  This may occur after an upgrade of Ambari, when" +
          "attempting to export a Blueprint from a cluster started by an older version of " +
          "Ambari.");
      }

    }

    /**
     * Get configuration type.
     *
     * @return configuration type
     */
    public String getType() {
      return type;
    }

    /**
     * Get configuration tag.
     *
     * @return configuration tag
     */
    public String getTag() {
      return tag;
    }

    /**
     * Get configuration properties.
     *
     * @return map of properties and values
     */
    public Map<String, String> getProperties() {
      return properties;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Configuration that = (Configuration) o;
      return tag.equals(that.tag) && type.equals(that.type) && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
      int result = type.hashCode();
      result = 31 * result + tag.hashCode();
      result = 31 * result + properties.hashCode();
      return result;
    }

    /**
     * Strip required properties from configuration.
     *
     * @param properties  property map
     */
    private void stripRequiredProperties(Map<String, String> properties) {
      Iterator<Map.Entry<String, String>> iter = properties.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry<String, String> entry = iter.next();
        String property = entry.getKey();
        String category = getType();
        Collection<String> categoryProperties = propertiesToStrip.get(category);
        if (categoryProperties != null && categoryProperties.contains(property)) {
          iter.remove();
        }
      }
    }
  }

  // ----- Blueprint Post Processor inner class ------------------------------

  /**
   * Post processor that strips href properties
   */
  private static class BlueprintPostProcessor extends ResultPostProcessorImpl {
    private BlueprintPostProcessor(Request request) {
      super(request);
    }

    @Override
    protected void finalizeNode(TreeNode<Resource> node) {
      node.removeProperty("href");
    }
  }
}
