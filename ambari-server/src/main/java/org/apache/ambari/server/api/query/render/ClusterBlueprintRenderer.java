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
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.services.ResultPostProcessorImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renderer which renders a cluster resource as a blueprint.
 */
public class ClusterBlueprintRenderer extends BaseRenderer implements Renderer {

  // ----- Renderer ----------------------------------------------------------

  @Override
  public TreeNode<Set<String>> finalizeProperties(
      TreeNode<QueryInfo> queryProperties, boolean isCollection) {

    Set<String> properties = new HashSet<String>(queryProperties.getObject().getProperties());
    TreeNode<Set<String>> resultTree = new TreeNodeImpl<Set<String>>(
        null, properties, queryProperties.getName());

    copyPropertiesToResult(queryProperties, resultTree);
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
    String clusterName = (String) clusterResource.getPropertyValue(
        PropertyHelper.getPropertyId("Clusters", "cluster_name"));
    //todo: deal with name collision?
    String blueprintName = "blueprint-" + clusterName;
    String[] stackTokens = ((String) clusterResource.getPropertyValue(
            PropertyHelper.getPropertyId("Clusters", "version"))).split("-");

    blueprintResource.setProperty("Blueprints/blueprint_name", blueprintName);
    blueprintResource.setProperty("Blueprints/stack_name", stackTokens[0]);
    blueprintResource.setProperty("Blueprints/stack_version", stackTokens[1]);
    blueprintResource.setProperty(
        "host_groups", processHostGroups(clusterNode.getChild("hosts")));

    return blueprintResource;
  }

  /**
   * Process host group information for all hosts.
   *
   * @param hostNode a host node
   *
   * @return list of host group property maps, one element for each host group
   */
  private List<Map<String, Object>> processHostGroups(TreeNode<Resource> hostNode) {
    Map<HostGroup, HostGroup> mapHostGroups = new HashMap<HostGroup, HostGroup>();
    for (TreeNode<Resource> host : hostNode.getChildren()) {
      HostGroup group = HostGroup.parse(host);
      if (mapHostGroups.containsKey(group)) {
        mapHostGroups.get(group).incrementCardinality();
      } else {
        mapHostGroups.put(group, group);
      }
    }

    int count = 1;
    List<Map<String, Object>> listHostGroups = new ArrayList<Map<String, Object>>();
    for (HostGroup group : mapHostGroups.values()) {
      String groupName = "host_group_" + count++;
      Map<String, Object> mapGroupProperties = new HashMap<String, Object>();
      listHostGroups.add(mapGroupProperties);

      mapGroupProperties.put("name", groupName);
      mapGroupProperties.put("cardinality", String.valueOf(group.getCardinality()));
      mapGroupProperties.put("components", processHostGroupComponents(group));
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

  // ----- Host Group inner class --------------------------------------------

  /**
   * Host Group representation.
   */
  private static class HostGroup {
    /**
     * Associated components.
     */
    private Set<String> m_components = new HashSet<String>();

    /**
     * Number of instances.
     */
    private int m_cardinality = 1;

    /**
     * Factory method for obtaining a host group instance.
     * Parses a host tree node for host related information.
     *
     * @param host  host tree node
     *
     * @return a new HostGroup instance
     */
    public static HostGroup parse(TreeNode<Resource> host) {
      HostGroup group = new HostGroup();

      TreeNode<Resource> components = host.getChild("host_components");
      for (TreeNode<Resource> component : components.getChildren()) {
        group.getComponents().add((String) component.getObject().getPropertyValue(
            "HostRoles/component_name"));
      }

      group.addAmbariComponentIfLocalhost((String) host.getObject().getPropertyValue(
          PropertyHelper.getPropertyId("Hosts", "host_name")));

      return group;
    }

    /**                                                           `
     * Obtain associated components.
     *
     * @return set of associated components
     */
    public Set<String> getComponents() {
      return m_components;
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

      HostGroup hostGroup = (HostGroup) o;

      return m_components.equals(hostGroup.m_components);
    }

    @Override
    public int hashCode() {
      return m_components.hashCode();
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
