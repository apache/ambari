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

package org.apache.ambari.server.api.resources;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.internal.PropertyIdImpl;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.api.util.TreeNode;

import java.util.*;

/**
 * Host_Component resource definition.
 */
public class HostComponentResourceDefinition extends BaseResourceDefinition {

  /**
   * value of cluster id foreign key
   */
  private String m_clusterId;


  /**
   * Constructor.
   *
   * @param id        value of host_component id
   * @param clusterId value of cluster id foreign key
   * @param hostId    value of host id foreign key
   */
  public HostComponentResourceDefinition(String id, String clusterId, String hostId) {
    super(Resource.Type.HostComponent, id);
    m_clusterId = clusterId;
    setResourceId(Resource.Type.Cluster, m_clusterId);
    setResourceId(Resource.Type.Host, hostId);
  }

  @Override
  public String getPluralName() {
    return "host_components";
  }

  @Override
  public String getSingularName() {
    return "host_component";
  }


  @Override
  public Map<String, ResourceDefinition> getSubResources() {
    Map<String, ResourceDefinition> mapChildren = new HashMap<String, ResourceDefinition>();

    ComponentResourceDefinition componentResource = new ComponentResourceDefinition(
        getId(), m_clusterId, null);
    PropertyId serviceIdProperty = getClusterController().getSchema(
        Resource.Type.Component).getKeyPropertyId(Resource.Type.Service);
    componentResource.getQuery().addProperty(serviceIdProperty);
    mapChildren.put(componentResource.getSingularName(), componentResource);

    return mapChildren;
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = new ArrayList<PostProcessor>();
    listProcessors.add(new HostComponentHrefProcessor());
    listProcessors.add(new HostComponentHostProcessor());

    return listProcessors;
  }

  @Override
  public void setParentIds(Map<Resource.Type, String> mapIds) {
    String id = mapIds.remove(Resource.Type.Component);
    if (id != null) {
      setId(id);
    }
    super.setParentIds(mapIds);
  }


  /**
   * Host_Component resource processor which is responsible for generating href's for host components.
   * This is called by the ResultPostProcessor during post processing of a result.
   */
  private class HostComponentHrefProcessor extends BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      TreeNode<Resource> parent = resultNode.getParent();

      if (parent.getParent() != null && parent.getParent().getObject().getType() == Resource.Type.Component) {
        Resource r = resultNode.getObject();
        Schema schema = ClusterControllerHelper.getClusterController().getSchema(r.getType());
        Object host = r.getPropertyValue(schema.getKeyPropertyId(Resource.Type.Host));
        Object hostComponent = r.getPropertyValue(schema.getKeyPropertyId(r.getType()));

        href = href.substring(0, href.indexOf("/services/") + 1) +
            "hosts/" + host + "/host_components/" + hostComponent;

        resultNode.setProperty("href", href);
      } else {
        super.process(request, resultNode, href);
      }
    }
  }

  /**
   * Host_Component resource processor which is responsible for generating a host section for host components.
   * This is called by the ResultPostProcessor during post processing of a result.
   */
  private class HostComponentHostProcessor implements PostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      //todo: look at partial request fields to ensure that hosts should be returned
      if (request.getResourceDefinition().getType() == getType()) {
        // only add host if query host_resource was directly queried
        String nodeHref = resultNode.getProperty("href");
        resultNode.getObject().setProperty(new PropertyIdImpl("href", "host", false),
            nodeHref.substring(0, nodeHref.indexOf("/host_components/")));
      }
    }
  }
}
