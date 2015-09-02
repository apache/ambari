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

import com.sun.jersey.core.util.Base64;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WidgetLayout Service
 */
public class WidgetLayoutService extends BaseService {
  
  private final String clusterName;

  public  WidgetLayoutService(String clusterName) {
    this.clusterName = clusterName;
  }

  @GET
  @Path("{widgetLayoutId}")
  @Produces("text/plain")
  public Response getService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("widgetLayoutId") String widgetLayoutId) {

    return handleRequest(headers, body, ui, Request.Type.GET,
            createResource(widgetLayoutId));
  }

  /**
   * Handles URL: /widget_layouts
   * Get all instances for a view.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return instance collection resource representation
   */
  @GET
  @Produces("text/plain")
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
            createResource(null));
  }

  @POST
  @Path("{widgetLayoutId}")
  @Produces("text/plain")
  public Response createService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("widgetLayoutId") String widgetLayoutId) {
    return handleRequest(headers, body, ui, Request.Type.POST,
            createResource(widgetLayoutId));
  }

  @POST
  @Produces("text/plain")
  public Response createServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
            createResource(null));
  }

  @PUT
  @Path("{widgetLayoutId}")
  @Produces("text/plain")
  public Response updateService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("widgetLayoutId") String widgetLayoutId) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(widgetLayoutId));
  }

  @PUT
  @Produces("text/plain")
  public Response updateServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(null));
  }

  @DELETE
  @Path("{widgetLayoutId}")
  @Produces("text/plain")
  public Response deleteService(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("widgetLayoutId") String widgetLayoutId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(widgetLayoutId));
  }

  private ResourceInstance createResource(String widgetLayoutId) {
    Map<Resource.Type,String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.WidgetLayout, widgetLayoutId);
    mapIds.put(Resource.Type.Cluster, clusterName);
    return createResource(Resource.Type.WidgetLayout, mapIds);
  }
}
