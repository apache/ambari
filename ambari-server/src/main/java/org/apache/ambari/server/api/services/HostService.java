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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Service responsible for hosts resource requests.
 */
@Path("/hosts/")
public class HostService extends BaseService {

  /**
   * Parent cluster id.
   */
  private String m_clusterName;

  /**
   * Constructor.
   */
  public HostService() {
  }

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public HostService(String clusterName) {
    m_clusterName = clusterName;
  }

  /**
   * Handles GET /clusters/{clusterID}/hosts/{hostID} and /hosts/{hostID}
   * Get a specific host.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param hostName host id
   * @return host resource representation
   */
  @GET
  @Path("{hostName}")
  @Produces("text/plain")
  public Response getHost(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                          @PathParam("hostName") String hostName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createHostResource(m_clusterName, hostName, ui));
  }

  /**
   * Handles GET /clusters/{clusterID}/hosts and /hosts
   * Get all hosts for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return host collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createHostResource(m_clusterName, null, ui));
  }

  /**
   * Handles POST /clusters/{clusterID}/hosts
   * Create hosts by specifying an array of hosts in the http body.
   * This is used to create multiple hosts in a single request.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return status code only, 201 if successful
   */
  @POST
  @Produces("text/plain")
  public Response createHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createHostResource(m_clusterName, null, ui));
  }

  /**
   * Handles POST /clusters/{clusterID}/hosts/{hostID}
   * Create a specific host.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   * @param hostName host id
   *
   * @return host resource representation
   */
  @POST
  @Path("{hostName}")
  @Produces("text/plain")
  public Response createHost(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                          @PathParam("hostName") String hostName) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createHostResource(m_clusterName, hostName, ui));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts/{hostID}
   * Updates a specific host.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   * @param hostName host id
   *
   * @return information regarding updated host
   */
  @PUT
  @Path("{hostName}")
  @Produces("text/plain")
  public Response updateHost(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                          @PathParam("hostName") String hostName) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createHostResource(m_clusterName, hostName, ui));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts
   * Updates multiple hosts.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return information regarding updated host
   */
  @PUT
  @Produces("text/plain")
  public Response updateHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createHostResource(m_clusterName, null, ui));
  }

  /**
   * Handles DELETE /clusters/{clusterID}/hosts/{hostID}
   * Deletes a specific host.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param hostName host id
   *
   * @return host resource representation
   */
  @DELETE
  @Path("{hostName}")
  @Produces("text/plain")
  public Response deleteHost(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("hostName") String hostName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE,
        createHostResource(m_clusterName, hostName, ui));
  }

  @DELETE
  @Produces("text/plain")
  public Response deleteHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.DELETE,
            createHostResource(m_clusterName, null, ui));
  }

  /**
   * Get the host_components sub-resource.
   *
   * @param hostName host id
   * @return the host_components service
   */
  @Path("{hostName}/host_components")
  public HostComponentService getHostComponentHandler(@PathParam("hostName") String hostName) {
    return new HostComponentService(m_clusterName, hostName);
  }

  /**
   * Get the kerberos_identities sub-resource.
   *
   * @param hostName host id
   * @return the host_components service
   */
  @Path("{hostName}/kerberos_identities")
  public HostKerberosIdentityService getHostKerberosIdentityHandler(@PathParam("hostName") String hostName) {
    return new HostKerberosIdentityService(m_clusterName, hostName);
  }

  /**
   * Get the alerts sub-resource.
   *
   * @param hostName host id
   * @return the alerts service
   */
  @Path("{hostName}/alerts")
  public AlertService getAlertHandler(@PathParam("hostName") String hostName) {
    return new AlertService(m_clusterName, null, hostName);
  }

  /**
   * Gets the alert history service
   *
   * @param request
   *          the request
   * @param hostName
   *          the host name
   *
   * @return the alert history service
   */
  @Path("{hostName}/alert_history")
  public AlertHistoryService getAlertHistoryService(
      @Context javax.ws.rs.core.Request request,
      @PathParam("hostName") String hostName) {

    return new AlertHistoryService(m_clusterName, null, hostName);
  }

  /**
   * Gets the host stack versions service.
   *
   * @param request
   *          the request
   * @param hostName
   *          the host name
   *
   * @return the host stack versions service
   */
  @Path("{hostName}/stack_versions")
  public HostStackVersionService getHostStackVersionService(@Context javax.ws.rs.core.Request request,
      @PathParam("hostName") String hostName) {

    return new HostStackVersionService(hostName, m_clusterName);
  }

  /**
   * Create a service resource instance.
   *
   *
   *
   * @param clusterName  cluster
   * @param hostName     host name
   * @param ui           uri information
   *
   * @return a host resource instance
   */
  ResourceInstance createHostResource(String clusterName, String hostName, UriInfo ui) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Host, hostName);
    if (clusterName != null) {
      mapIds.put(Resource.Type.Cluster, clusterName);
    }

    return createResource(Resource.Type.Host, mapIds);
  }
}
