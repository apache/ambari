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

import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.api.util.TreeNode;

import java.util.*;

/**
 * Component resource definition.
 */
public class ComponentResourceDefinition extends BaseResourceDefinition {

  /**
   * value of clusterId foreign key
   */
  private String m_clusterId;

  /**
   * value of serviceId foreign key
   */
  private String m_serviceId;


  /**
   * Constructor.
   *
   * @param id        value of component id
   * @param clusterId value of cluster id
   * @param serviceId value of service id
   */
  public ComponentResourceDefinition(String id, String clusterId, String serviceId) {
    super(Resource.Type.Component, id);
    m_clusterId = clusterId;
    m_serviceId = serviceId;
    setResourceId(Resource.Type.Cluster, m_clusterId);
    setResourceId(Resource.Type.Service, m_serviceId);
  }

  @Override
  public String getPluralName() {
    return "components";
  }

  @Override
  public String getSingularName() {
    return "component";
  }


  @Override
  public Map<String, ResourceDefinition> getSubResources() {
    Map<String, ResourceDefinition> mapChildren = new HashMap<String, ResourceDefinition>();

    // for host_component collection need host id property
    HostComponentResourceDefinition hostComponentResource = new HostComponentResourceDefinition(
        getId(), m_clusterId, null);
    PropertyId hostIdProperty = getClusterController().getSchema(
        Resource.Type.HostComponent).getKeyPropertyId(Resource.Type.Host);
    hostComponentResource.getQuery().addProperty(hostIdProperty);
    mapChildren.put(hostComponentResource.getPluralName(), hostComponentResource);
    return mapChildren;

  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = super.getPostProcessors();
    listProcessors.add(new ComponentHrefProcessor());

    return listProcessors;
  }

  @Override
  public void setParentId(Resource.Type type, String value) {
    if (type == Resource.Type.HostComponent) {
      setId(value);
    } else {
      super.setParentId(type, value);
    }
  }

  /**
   * Base resource processor which generates href's.  This is called by the {@link org.apache.ambari.server.api.services.ResultPostProcessor} during post
   * processing of a result.
   */
  private class ComponentHrefProcessor extends BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      TreeNode<Resource> parent = resultNode.getParent();

      if (parent.getParent() != null && parent.getParent().getObject().getType() == Resource.Type.HostComponent) {
        Resource r = resultNode.getObject();
        String clusterId = getResourceIds().get(Resource.Type.Cluster);
        Schema schema = ClusterControllerHelper.getClusterController().getSchema(r.getType());
        String serviceId = r.getPropertyValue(schema.getKeyPropertyId(Resource.Type.Service));
        String componentId = r.getPropertyValue(schema.getKeyPropertyId(r.getType()));

        href = href.substring(0, href.indexOf(clusterId) + clusterId.length() + 1) +
            "services/" + serviceId + "/components/" + componentId;

        resultNode.setProperty("href", href);
      } else {
        super.process(request, resultNode, href);
      }
    }
  }
}
