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

import org.apache.ambari.api.resource.HostComponentResourceDefinition;
import org.apache.ambari.api.resource.ResourceDefinition;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
   * Handles URL: /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
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

    return handleRequest(headers, ui, Request.RequestType.GET,
        createResourceDefinition(hostComponentName, m_clusterName, m_hostName));
  }

  /**
   * Handles URL: /clusters/{clusterID}/hosts/{hostID}/host_components/
   * Get all host components for a host.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return host_component collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getHostComponents(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, ui, Request.RequestType.GET,
        createResourceDefinition(null, m_clusterName, m_hostName));
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
