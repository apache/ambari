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

package org.apache.ambari.api.services;

import org.apache.ambari.api.resource.ComponentResourceDefinition;
import org.apache.ambari.api.resource.ResourceDefinition;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

/**
 * Service responsible for components resource requests.
 */
public class ComponentService extends BaseService {
  /**
   * Parent cluster id.
   */
  private String m_clusterName;

  /**
   * Parent service id.
   */
  private String m_serviceName;

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   * @param serviceName service id
   */
  public ComponentService(String clusterName, String serviceName) {
    m_clusterName = clusterName;
    m_serviceName = serviceName;
  }

  /**
   * Handles URL: /clusters/{clusterID}/services/{serviceID}/components/{componentID}
   * Get a specific component.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param componentName component id
   * @return a component resource representation
   */
  @GET
  @Path("{componentName}")
  @Produces("text/plain")
  public Response getComponent(@Context HttpHeaders headers, @Context UriInfo ui,
                               @PathParam("componentName") String componentName) {

    return handleRequest(headers, ui, Request.Type.GET,
        createResourceDefinition(componentName, m_clusterName, m_serviceName));
  }

  /**
   * Handles URL: /clusters/{clusterID}/services/{serviceID}/components
   * Get all components for a service.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return component collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getComponents(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, ui, Request.Type.GET,
        createResourceDefinition(null, m_clusterName, m_serviceName));
  }

  /**
   * Create a component resource definition.
   *
   * @param clusterName   cluster name
   * @param serviceName   service name
   * @param componentName component name
   * @return a component resource definition
   */
  ResourceDefinition createResourceDefinition(String clusterName, String serviceName, String componentName) {
    return new ComponentResourceDefinition(clusterName, serviceName, componentName);
  }
}
