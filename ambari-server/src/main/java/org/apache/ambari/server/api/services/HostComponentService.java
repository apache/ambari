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

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.HostComponentResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceDefinition;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * Service responsible for host_components resource requests.
 */
public class HostComponentService extends BaseService {
  /**
   * Parent cluster id.
   */
  private String m_clusterName;

  /**
   * Parent host id.
   */
  private String m_hostName;

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   * @param hostName    host id
   */
  public HostComponentService(String clusterName, String hostName) {
    m_clusterName = clusterName;
    m_hostName = hostName;
  }

  /**
   * Handles GET /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Get a specific host_component.
   *
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentName host_component id
   * @return host_component resource representation
   */
  @GET
  @Path("{hostComponentName}")
  @Produces("text/plain")
  public Response getHostComponent(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("hostComponentName") String hostComponentName) {

    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceDefinition(hostComponentName, m_clusterName, m_hostName));
  }

  /**
   * Handles GET /clusters/{clusterID}/hosts/{hostID}/host_components/
   * Get all host components for a host.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return host_component collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getHostComponents(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceDefinition(null, m_clusterName, m_hostName));
  }

  /**
   * Handles POST /clusters/{clusterID}/hosts/{hostID}/host_components
   * Create host components by specifying an array of host components in the http body.
   * This is used to create multiple host components in a single request.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   *
   * @return status code only, 201 if successful
   */
  @POST
  @Produces("text/plain")
  public Response createHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createResourceDefinition(null, m_clusterName, m_hostName));
  }

  /**
   * Handles POST /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Create a specific host_component.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentName host_component id
   *
   * @return host_component resource representation
   */
  @POST
  @Path("{hostComponentName}")
  @Produces("text/plain")
  public Response createHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("hostComponentName") String hostComponentName) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createResourceDefinition(hostComponentName, m_clusterName, m_hostName));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Updates a specific host_component.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentName host_component id
   *
   * @return information regarding updated host_component
   */
  @PUT
  @Path("{hostComponentName}")
  @Produces("text/plain")
  public Response updateHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                      @PathParam("hostComponentName") String hostComponentName) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createResourceDefinition(hostComponentName, m_clusterName, m_hostName));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts/{hostID}/host_components
   * Updates multiple host_component resources.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   *
   * @return information regarding updated host_component resources
   */
  @PUT
  @Produces("text/plain")
  public Response updateHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createResourceDefinition(null, m_clusterName, m_hostName));
  }

  /**
   * Handles DELETE /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Delete a specific host_component.
   *
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentName host_component id
   *
   * @return host_component resource representation
   */
  @DELETE
  @Path("{hostComponentName}")
  @Produces("text/plain")
  public Response deleteHostComponent(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("hostComponentName") String hostComponentName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE,
        createResourceDefinition(hostComponentName, m_clusterName, m_hostName));
  }

  /**
   * Create a host_component resource definition.
   *
   * @param hostComponentName host_component name
   * @param clusterName       cluster name
   * @param hostName          host name
   * @return a host resource definition
   */
  ResourceDefinition createResourceDefinition(String hostComponentName, String clusterName, String hostName) {
    return new HostComponentResourceDefinition(hostComponentName, clusterName, hostName);
  }
}
