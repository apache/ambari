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

import com.google.inject.Inject;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.state.ConfigHelper;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
   * Handles GET: /clusters/{clusterID}/services/{serviceID}/components/{componentID}
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
  public Response getComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                               @PathParam("componentName") String componentName, @QueryParam("format") String format) {
    if (format != null && format.equals("client_config_tar")) {
      return createClientConfigResource(body, headers, ui, componentName);
    }

    return handleRequest(headers, body, ui, Request.Type.GET,
        createComponentResource(m_clusterName, m_serviceName, componentName));
  }

  /**
   * Handles GET: /clusters/{clusterID}/services/{serviceID}/components
   * Get all components for a service.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return component collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createComponentResource(m_clusterName, m_serviceName, null));
  }

  /**
   * Handles: POST /clusters/{clusterID}/services/{serviceID}/components
   * Create components by specifying an array of components in the http body.
   * This is used to create multiple components in a single request.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   *
   * @return status code only, 201 if successful
   */
  @POST
  @Produces("text/plain")
  public Response createComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createComponentResource(m_clusterName, m_serviceName, null));
  }

  /**
   * Handles: POST /clusters/{clusterID}/services/{serviceID}/components/{componentID}
   * Create a specific component.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   * @param componentName component id
   *
   * @return information regarding the created component
   */
  @POST
  @Path("{componentName}")
  @Produces("text/plain")
  public Response createComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("componentName") String componentName) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createComponentResource(m_clusterName, m_serviceName, componentName));
  }

  /**
   * Handles: PUT /clusters/{clusterID}/services/{serviceID}/components/{componentID}
   * Update a specific component.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   * @param componentName component id
   *
   * @return information regarding the updated component
   */
  @PUT
  @Path("{componentName}")
  @Produces("text/plain")
  public Response updateComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("componentName") String componentName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createComponentResource(
        m_clusterName, m_serviceName, componentName));
  }

  /**
   * Handles: PUT /clusters/{clusterID}/services/{serviceID}/components
   * Update multiple components.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   *
   * @return information regarding the updated component
   */
  @PUT
  @Produces("text/plain")
  public Response updateComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createComponentResource(
        m_clusterName, m_serviceName, null));
  }

  /**
   * Handles: DELETE /clusters/{clusterID}/services/{serviceID}/components/{componentID}
   * Delete a specific component.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param componentName cluster id
   * @return information regarding the deleted cluster
   */
  @DELETE
  @Path("{componentName}")
  @Produces("text/plain")
  public Response deleteComponent(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("componentName") String componentName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createComponentResource(
        m_clusterName, m_serviceName, componentName));
  }

  /**
   * Create a component resource instance.
   *
   *
   * @param clusterName   cluster name
   * @param serviceName   service name
   * @param componentName component name
   *
   * @return a component resource instance
   */
  ResourceInstance createComponentResource(String clusterName, String serviceName, String componentName) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Service, serviceName);
    mapIds.put(Resource.Type.Component, componentName);

    return createResource(Resource.Type.Component, mapIds);
  }

  private Response createClientConfigResource(String body, HttpHeaders headers, UriInfo ui,
                                      String componentName) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, m_clusterName);
    mapIds.put(Resource.Type.Service, m_serviceName);
    mapIds.put(Resource.Type.Component, componentName);


    Response response = handleRequest(headers, body, ui, Request.Type.GET,
            createResource(Resource.Type.ClientConfig, mapIds));

    //If response has errors return response
    if (response.getStatus() != 200) {
      return response;
    }

    Response.ResponseBuilder rb = Response.status(Response.Status.OK);
    Configuration configs = new Configuration();
    String tmpDir = configs.getProperty(Configuration.SERVER_TMP_DIR_KEY);
    File file = new File(tmpDir + File.separator + componentName + "-configs" + Configuration.DEF_ARCHIVE_EXTENSION);
    InputStream resultInputStream = null;
    try {
      resultInputStream = new FileInputStream(file);
    } catch (IOException e) {
      e.printStackTrace();
    }

    String contentType = Configuration.DEF_ARCHIVE_CONTENT_TYPE;
    String outputFileName = componentName + "-configs" + Configuration.DEF_ARCHIVE_EXTENSION;
    rb.header("Content-Disposition",  "attachment; filename=\"" + outputFileName + "\"");
    rb.entity(resultInputStream);
    return rb.type(contentType).build();

  }

}
