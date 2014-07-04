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
 * Service for stacks management.
 */
@Path("/stacks2/")
public class Stacks2Service extends BaseService {

  @GET
  @Produces("text/plain")
  public Response getStacks(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createStackResource(null));
  }

  @GET
  @Path("{stackName}")
  @Produces("text/plain")
  public Response getStack(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("stackName") String stackName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackResource(stackName));
  }

  @GET
  @Path("{stackName}/versions")
  @Produces("text/plain")
  public Response getStackVersions(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackVersionResource(stackName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}")
  @Produces("text/plain")
  public Response getStackVersion(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackVersionResource(stackName, stackVersion));
  }

  
  @GET
  @Path("{stackName}/versions/{stackVersion}/operatingSystems/{osType}/repositories")
  @Produces("text/plain")
  public Response getRepositories(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion,
      @PathParam("osType") String osType) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createRepositoryResource(stackName, stackVersion, osType, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/operatingSystems/{osType}/repositories/{repoId}")
  @Produces("text/plain")
  public Response getRepository(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion,
      @PathParam("osType") String osType,
      @PathParam("repoId") String repoId) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createRepositoryResource(stackName, stackVersion, osType, repoId));
  }
  
  @PUT
  @Path("{stackName}/versions/{stackVersion}/operatingSystems/{osType}/repositories/{repoId}")
  @Produces("text/plain")
  public Response updateRepository(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion,
      @PathParam("osType") String osType,
      @PathParam("repoId") String repoId) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createRepositoryResource(stackName, stackVersion, osType, repoId));
  }
  

  @GET
  @Path("{stackName}/versions/{stackVersion}/stackServices")
  @Produces("text/plain")
  public Response getStackServices(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceResource(stackName, stackVersion, null));
  }
  
  @GET
  @Path("{stackName}/versions/{stackVersion}/stackServices/{serviceName}")
  @Produces("text/plain")
  public Response getStackService(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion,
      @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceResource(stackName, stackVersion, serviceName));
  }

  
  @GET
  @Path("{stackName}/versions/{stackVersion}/stackServices/{serviceName}/configurations")
  @Produces("text/plain")
  public Response getStackConfigurations(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion,
      @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackConfigurationResource(stackName, stackVersion, serviceName, null));
  }
  
  
  @GET
  @Path("{stackName}/versions/{stackVersion}/stackServices/{serviceName}/configurations/{propertyName}")
  @Produces("text/plain")
  public Response getStackConfiguration(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion,
      @PathParam("serviceName") String serviceName,
      @PathParam("propertyName") String propertyName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackConfigurationResource(stackName, stackVersion, serviceName, propertyName));
  }
  
  @GET
  @Path("{stackName}/versions/{stackVersion}/stackServices/{serviceName}/serviceComponents")
  @Produces("text/plain")
  public Response getServiceComponents(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion,
      @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentResource(stackName, stackVersion, serviceName, null));
  }
  
  @GET
  @Path("{stackName}/versions/{stackVersion}/stackServices/{serviceName}/serviceComponents/{componentName}")
  @Produces("text/plain")
  public Response getServiceComponent(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion,
      @PathParam("serviceName") String serviceName,
      @PathParam("componentName") String componentName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentResource(stackName, stackVersion, serviceName, componentName));
  }
  
  
  @GET
  @Path("{stackName}/versions/{stackVersion}/operatingSystems")
  @Produces("text/plain")
  public Response getOperatingSystems(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createOperatingSystemResource(stackName, stackVersion, null));
  }
  
  @GET
  @Path("{stackName}/versions/{stackVersion}/operatingSystems/{osType}")
  @Produces("text/plain")
  public Response getOperatingSystem(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion,
      @PathParam("osType") String osType) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createOperatingSystemResource(stackName, stackVersion, osType));
  }
  
  
  ResourceInstance createOperatingSystemResource(String stackName,
      String stackVersion, String osType) {
    
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.OperatingSystem, osType);

    return createResource(Resource.Type.OperatingSystem, mapIds);
  }

  ResourceInstance createStackServiceComponentResource(
      String stackName, String stackVersion, String serviceName, String componentName) {

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackServiceComponent, componentName);

    return createResource(Resource.Type.StackServiceComponent, mapIds);
  }

  ResourceInstance createStackConfigurationResource(String stackName,
      String stackVersion, String serviceName, String propertyName) {
    
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackConfiguration, propertyName);

    return createResource(Resource.Type.StackConfiguration, mapIds);
  }

  ResourceInstance createStackServiceResource(String stackName,
      String stackVersion, String serviceName) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);

    return createResource(Resource.Type.StackService, mapIds);
  }

  ResourceInstance createRepositoryResource(String stackName,
      String stackVersion, String osType, String repoId) {

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.OperatingSystem, osType);
    mapIds.put(Resource.Type.Repository, repoId);

    return createResource(Resource.Type.Repository, mapIds);
  }

  ResourceInstance createStackVersionResource(String stackName,
      String stackVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);

    return createResource(Resource.Type.StackVersion, mapIds);
  }

  ResourceInstance createStackResource(String stackName) {

    return createResource(Resource.Type.Stack,
        Collections.singletonMap(Resource.Type.Stack, stackName));

  }
}
