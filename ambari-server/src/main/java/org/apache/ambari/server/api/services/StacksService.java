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

/**
 * Service for stacks management.
 */
@Path("/stacks/")
public class StacksService extends BaseService {

  @GET
  @Produces("text/plain")
  public Response getStacks(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackResource(null));
  }

  @GET
  @Path("{stackName}")
  @Produces("text/plain")
  public Response getStack(String body, @Context HttpHeaders headers,
                           @Context UriInfo ui,
                           @PathParam("stackName") String stackName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackResource(stackName));
  }

  @GET
  @Path("{stackName}/versions")
  @Produces("text/plain")
  public Response getStackVersions(String body,
                                   @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("stackName") String stackName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackVersionResource(stackName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}")
  @Produces("text/plain")
  public Response getStackVersion(String body,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo ui, @PathParam("stackName") String stackName,
                                  @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackVersionResource(stackName, stackVersion));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/configurations")
  @Produces("text/plain")
  public Response getStackLevelConfigurations(String body, @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("stackName") String stackName,
                                   @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackLevelConfigurationsResource(stackName, stackVersion, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/configurations/{propertyName}")
  @Produces("text/plain")
  public Response getStackLevelConfiguration(String body, @Context HttpHeaders headers,
                                        @Context UriInfo ui, @PathParam("stackName") String stackName,
                                        @PathParam("stackVersion") String stackVersion,
                                        @PathParam("serviceName") String serviceName,
                                        @PathParam("propertyName") String propertyName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackLevelConfigurationsResource(stackName, stackVersion, propertyName));
  }


  @GET
  @Path("{stackName}/versions/{stackVersion}/services")
  @Produces("text/plain")
  public Response getStackServices(String body, @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("stackName") String stackName,
                                   @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceResource(stackName, stackVersion, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}")
  @Produces("text/plain")
  public Response getStackService(String body, @Context HttpHeaders headers,
                                  @Context UriInfo ui, @PathParam("stackName") String stackName,
                                  @PathParam("stackVersion") String stackVersion,
                                  @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceResource(stackName, stackVersion, serviceName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/artifacts")
  @Produces("text/plain")
  public Response getStackArtifacts(String body, @Context HttpHeaders headers,
                                              @Context UriInfo ui, @PathParam("stackName") String stackName,
                                              @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackArtifactsResource(stackName, stackVersion, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/artifacts/{artifactName}")
  @Produces("text/plain")
  public Response getStackArtifact(String body, @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("stackName") String stackName,
                                   @PathParam("stackVersion") String stackVersion,
                                   @PathParam("artifactName") String artifactName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackArtifactsResource(stackName, stackVersion, artifactName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/artifacts")
  @Produces("text/plain")
  public Response getStackServiceArtifacts(String body, @Context HttpHeaders headers,
                                  @Context UriInfo ui, @PathParam("stackName") String stackName,
                                  @PathParam("stackVersion") String stackVersion,
                                  @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceArtifactsResource(stackName, stackVersion, serviceName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/themes")
  @Produces("text/plain")
  public Response getStackServiceThemes(String body, @Context HttpHeaders headers,
                                           @Context UriInfo ui, @PathParam("stackName") String stackName,
                                           @PathParam("stackVersion") String stackVersion,
                                           @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createStackServiceThemesResource(stackName, stackVersion, serviceName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/themes/{themeName}")
  @Produces("text/plain")
  public Response getStackServiceTheme(String body, @Context HttpHeaders headers,
                                           @Context UriInfo ui, @PathParam("stackName") String stackName,
                                           @PathParam("stackVersion") String stackVersion,
                                           @PathParam("serviceName") String serviceName,
                                           @PathParam("themeName") String themeName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createStackServiceThemesResource(stackName, stackVersion, serviceName, themeName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/artifacts/{artifactName}")
  @Produces("text/plain")
  public Response getStackServiceArtifact(String body, @Context HttpHeaders headers,
                                           @Context UriInfo ui, @PathParam("stackName") String stackName,
                                           @PathParam("stackVersion") String stackVersion,
                                           @PathParam("serviceName") String serviceName,
                                           @PathParam("artifactName") String artifactName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceArtifactsResource(stackName, stackVersion, serviceName, artifactName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/configurations")
  @Produces("text/plain")
  public Response getStackConfigurations(String body,
                                         @Context HttpHeaders headers,
                                         @Context UriInfo ui, @PathParam("stackName") String stackName,
                                         @PathParam("stackVersion") String stackVersion,
                                         @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackConfigurationResource(stackName, stackVersion, serviceName, null));
  }


  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/configurations/{propertyName}")
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
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/configurations/{propertyName}/dependencies")
  @Produces("text/plain")
  public Response getStackConfigurationDependencies(String body, @Context HttpHeaders headers,
                                        @Context UriInfo ui, @PathParam("stackName") String stackName,
                                        @PathParam("stackVersion") String stackVersion,
                                        @PathParam("serviceName") String serviceName,
                                        @PathParam("propertyName") String propertyName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackConfigurationDependencyResource(stackName, stackVersion, serviceName, propertyName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/components")
  @Produces("text/plain")
  public Response getServiceComponents(String body,
                                       @Context HttpHeaders headers,
                                       @Context UriInfo ui, @PathParam("stackName") String stackName,
                                       @PathParam("stackVersion") String stackVersion,
                                       @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentResource(stackName, stackVersion, serviceName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/components/{componentName}/dependencies")
  @Produces("text/plain")
  public Response getServiceComponentDependencies(String body, @Context HttpHeaders headers,
                                                  @Context UriInfo ui, @PathParam("stackName") String stackName,
                                                  @PathParam("stackVersion") String stackVersion,
                                                  @PathParam("serviceName") String serviceName,
                                                  @PathParam("componentName") String componentName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentDependencyResource(stackName, stackVersion, serviceName, componentName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/components/{componentName}/dependencies/{dependencyName}")
  @Produces("text/plain")
  public Response getServiceComponentDependency(String body, @Context HttpHeaders headers,
                                      @Context UriInfo ui, @PathParam("stackName") String stackName,
                                      @PathParam("stackVersion") String stackVersion,
                                      @PathParam("serviceName") String serviceName,
                                      @PathParam("componentName") String componentName,
                                      @PathParam("dependencyName") String dependencyName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentDependencyResource(stackName, stackVersion, serviceName, componentName, dependencyName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/components/{componentName}")
  @Produces("text/plain")
  public Response getServiceComponent(String body, @Context HttpHeaders headers,
                                      @Context UriInfo ui, @PathParam("stackName") String stackName,
                                      @PathParam("stackVersion") String stackVersion,
                                      @PathParam("serviceName") String serviceName,
                                      @PathParam("componentName") String componentName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentResource(stackName, stackVersion, serviceName, componentName));
  }

  /**
   * Handles ANY /{stackName}/versions/{stackVersion}/operating_systems.
   *
   * @param stackName stack name
   * @param stackVersion stack version
   * @return operating system service
   */
  @Path("{stackName}/versions/{stackVersion}/operating_systems")
  public OperatingSystemService getOperatingSystemsHandler(@PathParam("stackName") String stackName, @PathParam("stackVersion") String stackVersion) {
    final Map<Resource.Type, String> stackProperties = new HashMap<Resource.Type, String>();
    stackProperties.put(Resource.Type.Stack, stackName);
    stackProperties.put(Resource.Type.StackVersion, stackVersion);
    return new OperatingSystemService(stackProperties);
  }

  /**
   * Handles ANY /{stackName}/versions/{stackVersion}/repository_versions.
   *
   * @param stackName stack name
   * @param stackVersion stack version
   * @return repository version service
   */
  @Path("{stackName}/versions/{stackVersion}/repository_versions")
  public RepositoryVersionService getRepositoryVersionHandler(@PathParam("stackName") String stackName, @PathParam("stackVersion") String stackVersion) {
    final Map<Resource.Type, String> stackProperties = new HashMap<Resource.Type, String>();
    stackProperties.put(Resource.Type.Stack, stackName);
    stackProperties.put(Resource.Type.StackVersion, stackVersion);
    return new RepositoryVersionService(stackProperties);
  }

  /**
   * Handles ANY /{stackName}/versions/{stackVersion}/compatible_repository_versions.
   *
   * @param stackName stack name
   * @param stackVersion stack version
   * @return repository version service
   */
  @Path("{stackName}/versions/{stackVersion}/compatible_repository_versions")
  public CompatibleRepositoryVersionService getCompatibleRepositoryVersionHandler(
      @PathParam("stackName") String stackName,
      @PathParam("stackVersion") String stackVersion) {
    final Map<Resource.Type, String> stackProperties = new HashMap<Resource.Type, String>();
    stackProperties.put(Resource.Type.Stack, stackName);
    stackProperties.put(Resource.Type.StackVersion, stackVersion);
    return new CompatibleRepositoryVersionService(stackProperties);
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

  ResourceInstance createStackServiceComponentDependencyResource(
      String stackName, String stackVersion, String serviceName, String componentName, String dependencyName) {

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackServiceComponent, componentName);
    mapIds.put(Resource.Type.StackServiceComponentDependency, dependencyName);

    return createResource(Resource.Type.StackServiceComponentDependency, mapIds);
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

  ResourceInstance createStackConfigurationDependencyResource(String stackName,
                                                              String stackVersion, String serviceName, String propertyName) {

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackConfiguration, propertyName);

    return createResource(Resource.Type.StackConfigurationDependency, mapIds);
  }

  ResourceInstance createStackServiceResource(String stackName,
                                              String stackVersion, String serviceName) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);

    return createResource(Resource.Type.StackService, mapIds);
  }

  ResourceInstance createStackVersionResource(String stackName,
                                              String stackVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);

    return createResource(Resource.Type.StackVersion, mapIds);
  }

  ResourceInstance createStackLevelConfigurationsResource(String stackName,
      String stackVersion, String propertyName) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackLevelConfiguration, propertyName);

    return createResource(Resource.Type.StackLevelConfiguration, mapIds);
  }

  ResourceInstance createStackArtifactsResource(String stackName, String stackVersion, String artifactName) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackArtifact, artifactName);

    return createResource(Resource.Type.StackArtifact, mapIds);
  }

  ResourceInstance createStackServiceArtifactsResource(String stackName,
                                                       String stackVersion,
                                                       String serviceName,
                                                       String artifactName) {

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackArtifact, artifactName);

    return createResource(Resource.Type.StackArtifact, mapIds);
  }

  ResourceInstance createStackServiceThemesResource(String stackName, String stackVersion, String serviceName,
                                                    String themeName) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.Theme, themeName);

    return createResource(Resource.Type.Theme, mapIds);
  }

  ResourceInstance createStackResource(String stackName) {

    return createResource(Resource.Type.Stack,
        Collections.singletonMap(Resource.Type.Stack, stackName));

  }
}

