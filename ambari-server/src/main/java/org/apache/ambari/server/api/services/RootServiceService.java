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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

@Path("/services/")
public class RootServiceService extends BaseService {
  
  @GET
  @Produces("text/plain")
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createServiceResource(null));
  }
  
  @GET
  @Path("{serviceName}")
  @Produces("text/plain")
  public Response getService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createServiceResource(serviceName));
  }

  @GET
  @Path("{serviceName}/components/{componentName}/hostComponents")
  @Produces("text/plain")
  public Response getRootHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("serviceName") String serviceName,
      @PathParam("componentName") String componentName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createHostComponentResource(serviceName, null, componentName));
  }
  
  
  @GET
  @Path("{serviceName}/hosts/")
  @Produces("text/plain")
  public Response getRootHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createHostResource(null));
  }
  
  @GET
  @Path("{serviceName}/hosts/{hostName}")
  @Produces("text/plain")
  public Response getRootHost(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("hostName") String hostName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createHostResource(hostName));
  }
  
  
  protected ResourceInstance createHostResource(String hostName) {
    return createResource(Resource.Type.Host, Collections.<Resource.Type, String>singletonMap(Resource.Type.Host, hostName));
  }

  
  @GET
  @Path("{serviceName}/hosts/{hostName}/hostComponents/")
  @Produces("text/plain")
  public Response getRootHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("serviceName") String serviceName,
      @PathParam("hostName") String hostName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createHostComponentResource(serviceName, hostName, null));
  }
  
  @GET
  @Path("{serviceName}/hosts/{hostName}/hostComponents/{hostComponent}")
  @Produces("text/plain")
  public Response getRootHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("serviceName") String serviceName,
      @PathParam("hostName") String hostName,
      @PathParam("hostComponent") String hostComponent) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createHostComponentResource(serviceName, hostName, hostComponent));
  }
  
  
  protected ResourceInstance createHostComponentResource(String serviceName, String hostName, String componentName) {
    
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.RootService, serviceName);
    mapIds.put(Resource.Type.Host, hostName);
    mapIds.put(Resource.Type.RootServiceComponent, componentName);

    return createResource(Resource.Type.RootServiceHostComponent, mapIds);
  }

  @GET
  @Path("{serviceName}/components/")
  @Produces("text/plain")
  public Response getServiceComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createServiceComponentResource(serviceName, null));
  }
  
  @GET
  @Path("{serviceName}/components/{componentName}")
  @Produces("text/plain")
  public Response getServiceComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("serviceName") String serviceName,
      @PathParam("componentName") String componentName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createServiceComponentResource(serviceName, componentName));
  }
  
  protected ResourceInstance createServiceResource(String serviceName) {
    return createResource(Resource.Type.RootService,
        Collections.singletonMap(Resource.Type.RootService, serviceName));
  }
  
  protected ResourceInstance createServiceComponentResource(String serviceName,
      String componentName) {
    
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.RootService, serviceName);
    mapIds.put(Resource.Type.RootServiceComponent, componentName);

    return createResource(Resource.Type.RootServiceComponent, mapIds);
  }
}
