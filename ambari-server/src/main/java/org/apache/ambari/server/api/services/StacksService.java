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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.predicate.QueryLexer;
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

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createStackResource(stackName));
  }

  @GET
  @Path("{stackName}/versions")
  @Produces("text/plain")
  public Response getStackVersions(String body,
                                   @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("stackName") String stackName) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createStackVersionResource(stackName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}")
  @Produces("text/plain")
  public Response getStackVersion(String body,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo ui, @PathParam("stackName") String stackName,
                                  @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createStackVersionResource(stackName, stackVersion));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/operating_systems/{osType}/repositories")
  @Produces("text/plain")
  public Response getRepositories(String body,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo ui, @PathParam("stackName") String stackName,
                                  @PathParam("stackVersion") String stackVersion,
                                  @PathParam("osType") String osType) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createRepositoryResource(stackName, stackVersion, osType, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/operating_systems/{osType}/repositories/{repoId}")
  @Produces("text/plain")
  public Response getRepository(String body,
                                @Context HttpHeaders headers,
                                @Context UriInfo ui, @PathParam("stackName") String stackName,
                                @PathParam("stackVersion") String stackVersion,
                                @PathParam("osType") String osType,
                                @PathParam("repoId") String repoId) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createRepositoryResource(stackName, stackVersion, osType, repoId));
  }

  @PUT
  @Path("{stackName}/versions/{stackVersion}/operating_systems/{osType}/repositories/{repoId}")
  @Produces("text/plain")
  public Response updateRepository(String body, @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("stackName") String stackName,
                                   @PathParam("stackVersion") String stackVersion,
                                   @PathParam("osType") String osType,
                                   @PathParam("repoId") String repoId) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.PUT,
        createRepositoryResource(stackName, stackVersion, osType, repoId));
  }
  
  @GET
  @Path("{stackName}/versions/{stackVersion}/configurations")
  @Produces("text/plain")
  public Response getStackLevelConfigurations(String body, @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("stackName") String stackName,
                                   @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
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

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createStackLevelConfigurationsResource(stackName, stackVersion, propertyName));
  }


  @GET
  @Path("{stackName}/versions/{stackVersion}/services")
  @Produces("text/plain")
  public Response getStackServices(String body, @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("stackName") String stackName,
                                   @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createStackServiceResource(stackName, stackVersion, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}")
  @Produces("text/plain")
  public Response getStackService(String body, @Context HttpHeaders headers,
                                  @Context UriInfo ui, @PathParam("stackName") String stackName,
                                  @PathParam("stackVersion") String stackVersion,
                                  @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createStackServiceResource(stackName, stackVersion, serviceName));
  }


  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/configurations")
  @Produces("text/plain")
  public Response getStackConfigurations(String body,
                                         @Context HttpHeaders headers,
                                         @Context UriInfo ui, @PathParam("stackName") String stackName,
                                         @PathParam("stackVersion") String stackVersion,
                                         @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
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

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createStackConfigurationResource(stackName, stackVersion, serviceName, propertyName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/components")
  @Produces("text/plain")
  public Response getServiceComponents(String body,
                                       @Context HttpHeaders headers,
                                       @Context UriInfo ui, @PathParam("stackName") String stackName,
                                       @PathParam("stackVersion") String stackVersion,
                                       @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
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

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
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

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
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

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createStackServiceComponentResource(stackName, stackVersion, serviceName, componentName));
  }


  @GET
  @Path("{stackName}/versions/{stackVersion}/operating_systems")
  @Produces("text/plain")
  public Response getOperatingSystems(String body, @Context HttpHeaders headers,
                                      @Context UriInfo ui, @PathParam("stackName") String stackName,
                                      @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
        createOperatingSystemResource(stackName, stackVersion, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/operating_systems/{osType}")
  @Produces("text/plain")
  public Response getOperatingSystem(String body, @Context HttpHeaders headers,
                                     @Context UriInfo ui, @PathParam("stackName") String stackName,
                                     @PathParam("stackVersion") String stackVersion,
                                     @PathParam("osType") String osType) {

    return handleRequest(headers, body, new StackUriInfo(ui), Request.Type.GET,
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
  
  ResourceInstance createStackLevelConfigurationsResource(String stackName,
      String stackVersion, String propertyName) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackLevelConfiguration, propertyName);

    return createResource(Resource.Type.StackLevelConfiguration, mapIds);
  }

  ResourceInstance createStackResource(String stackName) {

    return createResource(Resource.Type.Stack,
        Collections.singletonMap(Resource.Type.Stack, stackName));

  }

  /**
   * Temporary UriInfo implementation which is used to convert old property names.
   * Because both the /stacks and /stacks2 api use the same underlying classes, we
   * need to convert the new corrected property names to the old names for the back end.
   * This should be removed when /stacks2 is removed and we can change the property names
   * in the resource definitions to the new form.
   */
  public static class StackUriInfo implements UriInfo {
    private UriInfo m_delegate;

    public StackUriInfo(UriInfo delegate) {
      m_delegate = delegate;
    }
    @Override
    public String getPath() {
      return m_delegate.getPath();
    }

    @Override
    public String getPath(boolean b) {
      return m_delegate.getPath(b);
    }

    @Override
    public List<PathSegment> getPathSegments() {
      return m_delegate.getPathSegments();
    }

    @Override
    public List<PathSegment> getPathSegments(boolean b) {
      return m_delegate.getPathSegments(b);
    }

    @Override
    public URI getRequestUri() {
      String uri;
      try {
        uri = URLDecoder.decode(m_delegate.getRequestUri().toASCIIString(), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("Unable to decode URI: " + e, e);
      }
      uri = uri.replaceAll("services/", "stackServices/");
      uri = uri.replaceAll("components/", "serviceComponents/");
      uri = uri.replaceAll("operating_systems/", "operatingSystems/");

      try {
        return new URI(uri);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Unable to create modified stacks URI: " + e, e);
      }
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
      return m_delegate.getRequestUriBuilder();
    }

    @Override
    public URI getAbsolutePath() {
      return m_delegate.getAbsolutePath();
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
      return m_delegate.getAbsolutePathBuilder();
    }

    @Override
    public URI getBaseUri() {
      return m_delegate.getBaseUri();
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
      return m_delegate.getBaseUriBuilder();
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
      return m_delegate.getPathParameters();
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean b) {
      return m_delegate.getPathParameters(b);
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
      MultivaluedMap<String, String> queryParams = m_delegate.getQueryParameters();

      if (queryParams.containsKey(QueryLexer.QUERY_FIELDS)) {
        String fields = queryParams.getFirst(QueryLexer.QUERY_FIELDS);
        queryParams.putSingle(QueryLexer.QUERY_FIELDS, convertToOldPropertyNames(fields));
      }

      if (queryParams.containsKey(QueryLexer.QUERY_SORT)) {
        String sortBy = queryParams.getFirst(QueryLexer.QUERY_SORT);
        queryParams.putSingle(QueryLexer.QUERY_SORT, convertToOldPropertyNames(sortBy));
      }

      return queryParams;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean b) {
      return m_delegate.getQueryParameters(b);
    }

    @Override
    public List<String> getMatchedURIs() {
      return m_delegate.getMatchedURIs();
    }

    @Override
    public List<String> getMatchedURIs(boolean b) {
      return m_delegate.getMatchedURIs(b);
    }

    @Override
    public List<Object> getMatchedResources() {
      return m_delegate.getMatchedResources();
    }

    private String convertToOldPropertyNames(String str) {
      str = str.replaceAll("services", "stackServices");
      str = str.replaceAll("components", "serviceComponents");
      str = str.replaceAll("operating_systems", "operatingSystems");

      return str;
    }
  }
}

