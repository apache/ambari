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

package org.apache.ambari.server.api.query.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.services.ResultPostProcessorImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.ArtifactResourceProvider;
import org.apache.ambari.server.controller.internal.BlueprintConfigurationProcessor;
import org.apache.ambari.server.controller.internal.BlueprintResourceProvider;
import org.apache.ambari.server.controller.internal.ExportBlueprintRequest;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.AmbariContext;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.ClusterTopologyImpl;
import org.apache.ambari.server.topology.Component;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

/**
 * Renderer which renders a cluster resource as a blueprint.
 */
public class ClusterBlueprintRenderer extends BaseRenderer implements Renderer {

  /**
   * Management Controller used to get stack information.
   */
  private AmbariManagementController controller = AmbariServer.getController();

//  /**
//   * Map of configuration type to configuration properties which are required that a user
//   * input.  These properties will be stripped from the exported blueprint.
//   */
//  private Map<String, Collection<String>> propertiesToStrip = new HashMap<String, Collection<String>>();

  private final static Logger LOG = LoggerFactory.getLogger(ClusterBlueprintRenderer.class);


  // ----- Renderer ----------------------------------------------------------

  @Override
  public TreeNode<Set<String>> finalizeProperties(
      TreeNode<QueryInfo> queryProperties, boolean isCollection) {

    Set<String> properties = new HashSet<>(queryProperties.getObject().getProperties());
    TreeNode<Set<String>> resultTree = new TreeNodeImpl<>(
      null, properties, queryProperties.getName());

    copyPropertiesToResult(queryProperties, resultTree);

    String configType = Resource.Type.Configuration.name();
    if (resultTree.getChild(configType) == null) {
      resultTree.addChild(new HashSet<>(), configType);
    }

    String serviceType = Resource.Type.Service.name();
    if (resultTree.getChild(serviceType) == null) {
      resultTree.addChild(new HashSet<>(), serviceType);
    }
    TreeNode<Set<String>> serviceNode = resultTree.getChild(serviceType);
    if (serviceNode == null) {
      serviceNode = resultTree.addChild(new HashSet<>(), serviceType);
    }
    String serviceComponentType = Resource.Type.Component.name();
    TreeNode<Set<String>> serviceComponentNode = resultTree.getChild(
      serviceType + "/" + serviceComponentType);
    if (serviceComponentNode == null) {
      serviceComponentNode = serviceNode.addChild(new HashSet<>(), serviceComponentType);
    }
    serviceComponentNode.getObject().add("ServiceComponentInfo/cluster_name");
    serviceComponentNode.getObject().add("ServiceComponentInfo/service_name");
    serviceComponentNode.getObject().add("ServiceComponentInfo/component_name");
    serviceComponentNode.getObject().add("ServiceComponentInfo/recovery_enabled");

    String hostType = Resource.Type.Host.name();
    String hostComponentType = Resource.Type.HostComponent.name();
    TreeNode<Set<String>> hostComponentNode = resultTree.getChild(
        hostType + "/" + hostComponentType);

    if (hostComponentNode == null) {
      TreeNode<Set<String>> hostNode = resultTree.getChild(hostType);
      if (hostNode == null) {
        hostNode = resultTree.addChild(new HashSet<>(), hostType);
      }
      hostComponentNode = hostNode.addChild(new HashSet<>(), hostComponentType);
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
      blueprintResultTree.addChild(new TreeNodeImpl<>(
        blueprintResultTree, blueprintResource, node.getName()));
    }
    return result;
  }

  @Override
  public ResultPostProcessor getResultPostProcessor(Request request) {
    return new BlueprintPostProcessor(request);
  }

  @Override
  public boolean requiresPropertyProviderInput() {
    // the Blueprint-based renderer does not require property provider input
    // this method will help to filter out the un-necessary calls to the AMS
    // and Alerts Property providers, since they are not included in the
    // exported Blueprint
    return false;
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
    Resource blueprintResource = new ResourceImpl(Resource.Type.Cluster);

    ClusterTopology topology;
    try {
      topology = createClusterTopology(clusterNode);
    } catch (InvalidTopologyTemplateException | InvalidTopologyException e) {
      //todo
      throw new RuntimeException("Unable to process blueprint export request: " + e, e);
    }

    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport();

    Set<StackId> stackIds = topology.getBlueprint().getStackIds();
    if (stackIds.size() == 1) {
      StackId stackId = Iterables.getOnlyElement(stackIds);
      blueprintResource.setProperty("Blueprints/stack_name", stackId.getStackName());
      blueprintResource.setProperty("Blueprints/stack_version", stackId.getStackVersion());
    }

    if (topology.isClusterKerberosEnabled()) {
      Map<String, Object> securityConfigMap = new LinkedHashMap<>();
      securityConfigMap.put(SecurityConfigurationFactory.TYPE_PROPERTY_ID, SecurityType.KERBEROS.name());

      try {
        String clusterName = topology.getAmbariContext().getClusterName(topology.getClusterId());
        Map<String, Object> kerberosDescriptor = getKerberosDescriptor(topology.getAmbariContext()
          .getClusterController(), clusterName);
        if (kerberosDescriptor != null) {
          securityConfigMap.put(SecurityConfigurationFactory.KERBEROS_DESCRIPTOR_PROPERTY_ID, kerberosDescriptor);
        }
      } catch (AmbariException e) {
        LOG.info("Unable to retrieve kerberos_descriptor: ", e.getMessage());
      }
      blueprintResource.setProperty(BlueprintResourceProvider.BLUEPRINT_SECURITY_PROPERTY_ID, securityConfigMap);
    }

    List<Map<String, Object>> groupList = formatGroupsAsList(topology);
    blueprintResource.setProperty("host_groups", groupList);

    //todo: ensure that this is properly handled in config processor
    //determinePropertiesToStrip(topology);

    blueprintResource.setProperty("configurations", processConfigurations(topology));

    //Fetch settings section for blueprint
    blueprintResource.setProperty("settings", getSettings(clusterNode));

    return blueprintResource;
  }

  /***
   * Constructs the Settings object of the following form:
   * "settings": [   {
   "recovery_settings": [
   {
   "recovery_enabled": "true"
   }   ]   },
   {
   "service_settings": [   {
   "name": "HDFS",
   "recovery_enabled": "true",
   "credential_store_enabled": "true"
   },
   {
   "name": "TEZ",
   "recovery_enabled": "false"
   },
   {
   "name": "HIVE",
   "recovery_enabled": "false"
   }   ]   },
   {
   "component_settings": [   {
   "name": "DATANODE",
   "recovery_enabled": "true"
   }   ]   }   ]
   *
   * @param clusterNode
   * @return A Collection<Map<String, Object>> which represents the Setting Object
   */
  private Collection<Map<String, Object>> getSettings(TreeNode<Resource> clusterNode) {
    LOG.info("ClusterBlueprintRenderer: getSettings()");

    //Initialize collections to create appropriate json structure
    Collection<Map<String, Object>> blueprintSetting = new ArrayList<>();

    Set<Map<String, String>> recoverySettingValue = new HashSet<>();
    Set<Map<String, String>> serviceSettingValue = new HashSet<>();
    Set<Map<String, String>> componentSettingValue = new HashSet<>();

    HashMap<String, String> property = new HashMap<>();
    HashMap<String, String> componentProperty = new HashMap<>();
    Boolean globalRecoveryEnabled = false;

    //Fetch the services, to obtain ServiceInfo and ServiceComponents
    Collection<TreeNode<Resource>> serviceChildren = clusterNode.getChild("services").getChildren();
    for (TreeNode serviceNode : serviceChildren) {
      ResourceImpl service = (ResourceImpl) serviceNode.getObject();
      Map<String, Object> ServiceInfoMap = service.getPropertiesMap().get("ServiceInfo");

      //service_settings population
      property = new HashMap<>();
      if (ServiceInfoMap.get("credential_store_enabled").equals("true")) {
        property.put("name", ServiceInfoMap.get("service_name").toString());
        property.put("credential_store_enabled", "true");
      }

      //Fetch the service Components to obtain ServiceComponentInfo
      Collection<TreeNode<Resource>> componentChildren = serviceNode.getChild("components").getChildren();
      for (TreeNode componentNode : componentChildren) {
        ResourceImpl component = (ResourceImpl) componentNode.getObject();
        Map<String, Object> ServiceComponentInfoMap = component.getPropertiesMap().get("ServiceComponentInfo");

        if (ServiceComponentInfoMap.get("recovery_enabled").equals("true")) {
          globalRecoveryEnabled = true;
          property.put("name", ServiceInfoMap.get("service_name").toString());
          property.put("recovery_enabled", "true");

          //component_settings population
          componentProperty = new HashMap<>();
          componentProperty.put("name", ServiceComponentInfoMap.get("component_name").toString());
          componentProperty.put("recovery_enabled", "true");
        }
      }

      if (!property.isEmpty())
        serviceSettingValue.add(property);
      if (!componentProperty.isEmpty())
        componentSettingValue.add(componentProperty);
    }
    //recovery_settings population
    property = new HashMap<>();
    if (globalRecoveryEnabled) {
      property.put("recovery_enabled", "true");
    } else {
      property.put("recovery_enabled", "false");
    }
    recoverySettingValue.add(property);

    //Add all the different setting values.
    Map<String, Object> settingMap = new HashMap<>();
    settingMap.put("recovery_settings", recoverySettingValue);
    blueprintSetting.add(settingMap);

    settingMap = new HashMap<>();
    settingMap.put("service_settings", serviceSettingValue);
    blueprintSetting.add(settingMap);

    settingMap = new HashMap<>();
    settingMap.put("component_settings", componentSettingValue);
    blueprintSetting.add(settingMap);

    return blueprintSetting;
  }

  private Map<String, Object> getKerberosDescriptor(ClusterController clusterController, String clusterName) throws AmbariException {
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property("Artifacts/cluster_name").equals(clusterName).and().
      property(ArtifactResourceProvider.ARTIFACT_NAME_PROPERTY).equals("kerberos_descriptor").
      end().toPredicate();

    ResourceProvider artifactProvider =
       clusterController.ensureResourceProvider(Resource.Type.Artifact);

    org.apache.ambari.server.controller.spi.Request request = new RequestImpl(Collections.emptySet(),
      Collections.emptySet(), Collections.emptyMap(), null);

    Set<Resource> response = null;
    try {
      response = artifactProvider.getResources(request, predicate);
    } catch (SystemException | UnsupportedPropertyException | NoSuchResourceException | NoSuchParentResourceException
      e) {
      throw new AmbariException("An unknown error occurred while trying to obtain the cluster kerberos descriptor", e);
    }

    if (response != null && !response.isEmpty()) {
      Resource descriptorResource = response.iterator().next();
      Map<String, Map<String, Object>> propertyMap = descriptorResource.getPropertiesMap();

      if (propertyMap != null) {
        Map<String, Object> artifactData = propertyMap.get(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY);
        Map<String, Object> artifactDataProperties = propertyMap.get(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY + "/properties");
        HashMap<String, Object> data = new HashMap<>();

        if (artifactData != null) {
          data.putAll(artifactData);
        }

        if (artifactDataProperties != null) {
          data.put("properties", artifactDataProperties);
        }
        return data;
      }
    }
    return null;
  }

  /**
   * Process cluster scoped configurations.
   *
   *
   * @return cluster configuration
   */
  private List<Map<String, Map<String, Map<String, ?>>>>  processConfigurations(ClusterTopology topology) {

    List<Map<String, Map<String, Map<String, ?>>>> configList = new ArrayList<>();

    Configuration configuration = topology.getConfiguration();
    Collection<String> allTypes = new HashSet<>();
    allTypes.addAll(configuration.getFullProperties().keySet());
    allTypes.addAll(configuration.getFullAttributes().keySet());
    for (String type : allTypes) {
      Map<String, Map<String, ?>> typeMap = new HashMap<>();
      typeMap.put("properties", configuration.getFullProperties().get(type));
      if (! configuration.getFullAttributes().isEmpty()) {
        typeMap.put("properties_attributes", configuration.getFullAttributes().get(type));
      }

      configList.add(Collections.singletonMap(type, typeMap));
    }

    return configList;
  }

  /**
   * Process host group information for all hosts.
   *
   *
   * @return list of host group property maps, one element for each host group
   */
  private List<Map<String, Object>> formatGroupsAsList(ClusterTopology topology) {
    List<Map<String, Object>> listHostGroups = new ArrayList<>();
    for (HostGroupInfo group : topology.getHostGroupInfo().values()) {
      Map<String, Object> mapGroupProperties = new HashMap<>();
      listHostGroups.add(mapGroupProperties);

      String name = group.getHostGroupName();
      mapGroupProperties.put("name", name);
      mapGroupProperties.put("cardinality", String.valueOf(group.getHostNames().size()));
      mapGroupProperties.put("components", processHostGroupComponents(topology.getBlueprint().getHostGroup(name)));

      Configuration configuration = topology.getHostGroupInfo().get(name).getConfiguration();
      List<Map<String, Map<String, String>>> configList = new ArrayList<>();
      for (Map.Entry<String, Map<String, String>> typeEntry : configuration.getProperties().entrySet()) {
        Map<String, Map<String, String>> propertyMap = Collections.singletonMap(
            typeEntry.getKey(), typeEntry.getValue());

        configList.add(propertyMap);
      }
      mapGroupProperties.put("configurations", configList);
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
  private List<Map<String, String>> processHostGroupComponents(HostGroup group) {
    List<Map<String, String>> listHostGroupComponents = new ArrayList<>();
    for (Component component : group.getComponents()) {
      Map<String, String> mapComponentProperties = new HashMap<>();
      listHostGroupComponents.add(mapComponentProperties);
      mapComponentProperties.put("name", component.getName());
    }
    return listHostGroupComponents;
  }

  protected ClusterTopology createClusterTopology(TreeNode<Resource> clusterNode)
      throws InvalidTopologyTemplateException, InvalidTopologyException {

    return new ClusterTopologyImpl(new AmbariContext(), new ExportBlueprintRequest(clusterNode));
  }

  /**
   * Determine whether a node represents a collection.
   *
   * @param node  node which is evaluated for being a collection
   *
   * @return true if the node represents a collection; false otherwise
   */
  private boolean isCollection(TreeNode<Resource> node) {
    String isCollection = node.getStringProperty("isCollection");
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


  //  /**
//   * Determine which configuration properties need to be stripped from the configuration prior to exporting.
//   * Stripped properties are any property which are marked as required in the stack definition.  For example,
//   * all passwords are required properties and are therefore not exported.
//   *
//   * @param servicesNode  services node
//   * @param stackName     stack name
//   * @param stackVersion  stack version
//   */
//  private void determinePropertiesToStrip(TreeNode<Resource> servicesNode, String stackName, String stackVersion) {
//    AmbariMetaInfo ambariMetaInfo = getController().getAmbariMetaInfo();
//    StackInfo stack;
//    try {
//      stack = ambariMetaInfo.getStack(stackName, stackVersion);
//    } catch (AmbariException e) {
//      // shouldn't ever happen.
//      // Exception indicates that stack is not defined
//      // but we are getting the stack name from a running cluster.
//      throw new RuntimeException("Unexpected exception occurred while generating a blueprint. "  +
//          "The stack '" + stackName + ":" + stackVersion + "' does not exist");
//    }
//    Map<String, PropertyInfo> requiredStackProperties = stack.getRequiredProperties();
//    updatePropertiesToStrip(requiredStackProperties);
//
//    for (TreeNode<Resource> serviceNode : servicesNode.getChildren()) {
//      String name = (String) serviceNode.getObject().getPropertyValue("ServiceInfo/service_name");
//      ServiceInfo service;
//      try {
//        service = ambariMetaInfo.getService(stackName, stackVersion, name);
//      } catch (AmbariException e) {
//        // shouldn't ever happen.
//        // Exception indicates that service is not in the stack
//        // but we are getting the name from a running cluster.
//        throw new RuntimeException("Unexpected exception occurred while generating a blueprint.  The service '" +
//            name + "' was not found in the stack: '" + stackName + ":" + stackVersion);
//      }
//
//      Map<String, PropertyInfo> requiredProperties = service.getRequiredProperties();
//      updatePropertiesToStrip(requiredProperties);
//    }
//  }

//  /**
//   * Helper method to update propertiesToStrip with properties that are marked as required
//   *
//   * @param requiredProperties  Properties marked as required
//   */
//  private void updatePropertiesToStrip(Map<String, PropertyInfo> requiredProperties) {
//
//    for (Map.Entry<String, PropertyInfo> entry : requiredProperties.entrySet()) {
//      String propertyName = entry.getKey();
//      PropertyInfo propertyInfo = entry.getValue();
//      String configCategory = propertyInfo.getFilename();
//      if (configCategory.endsWith(".xml")) {
//        configCategory = configCategory.substring(0, configCategory.indexOf(".xml"));
//      }
//      Collection<String> categoryProperties = propertiesToStrip.get(configCategory);
//      if (categoryProperties == null) {
//        categoryProperties = new ArrayList<String>();
//        propertiesToStrip.put(configCategory, categoryProperties);
//      }
//      categoryProperties.add(propertyName);
//    }
//  }





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
