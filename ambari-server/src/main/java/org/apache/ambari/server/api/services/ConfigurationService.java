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

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ConfigurationResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceDefinition;

/**
 * Service responsible for services resource requests.
 */
public class ConfigurationService extends BaseService {
  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public ConfigurationService(String clusterName) {
    m_clusterName = clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterId}/configurations
   * Get all services for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return service collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getConfigurations(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceDefinition(null, null, m_clusterName));
  }  

  /**
   * Handles URL: /clusters/{clusterId}/configurations
   * Get all services for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return service collection resource representation
   */
  @PUT
  @Produces("text/plain")
  public Response createConfigurations(String body,@Context HttpHeaders headers, @Context UriInfo ui) {
    
    return handleRequest(headers, body, ui, Request.Type.PUT,
        createResourceDefinition(null, null, m_clusterName));
  }    
  
  /*
   * Handles URL: /clusters/{clusterID}/configurations/{configType}
   * Get a specific service.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param configType service id
   * @return service resource representation
  
  @GET
  @Path("{configType}")
  @Produces("text/plain")
  public Response getService(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("configType") String configType) {

    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceDefinition(configType, null, m_clusterName));
  }
   */
  
  /*
   * Handles URL: /clusters/{clusterId}/configurations/{configType}/{configTag}
   *
  @GET
  @Path("{configType}/{configTag}")
  @Produces("text/plain")
  public Response getConfig(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("configType") String configType,
                             @PathParam("configTag") String configTag) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceDefinition(configType, configTag, m_clusterName));    
  }
   */



  /*
   * Handles: PUT /clusters/{clusterId}/configurations/{configType}
   * Create a specific service.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @param serviceName service id
   * @return information regarding the created service
   *
  @PUT
  @Path("{configType}")
  @Produces("text/plain")
  public Response createConfig(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("configType") String configType) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createResourceDefinition(configType, null, m_clusterName));
  }
  
   */


  /**
   * Create a service resource definition.
   *
   * @param serviceName host name
   * @param clusterName cluster name
   * @return a service resource definition
   */
  ResourceDefinition createResourceDefinition(String configType, String configTag, String clusterName) {
    return new ConfigurationResourceDefinition(configType, configTag, clusterName);
  }
}
