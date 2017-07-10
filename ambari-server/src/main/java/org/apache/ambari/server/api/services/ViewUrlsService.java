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
import org.apache.ambari.server.security.authorization.AuthorizationException;

import com.google.common.base.Optional;


/**
 * Service responsible for view resource requests.
 */
@Path("/view/urls")
public class ViewUrlsService extends BaseService {

  /**
   * Get the list of all registered view URLs
   * @param headers
   * @param ui

   * @return collections of all view urls and any instances registered against them
   */
  @GET
  @Produces("text/plain")
  public Response getViewUrls(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createViewUrlResource(Optional.<String>absent()));
  }


  /**
   * Create a new View URL
   * @param body
   * @param headers
   * @param ui
   * @param urlName
   * @return
   * @throws AuthorizationException
     */
  @POST
  @Path("{urlName}")
  @Produces("text/plain")
  public Response createUrl(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("urlName") String urlName) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.POST, createViewUrlResource(Optional.of(urlName)));
  }


  /**
   * Update a view URL
   * @param body
   * @param headers
   * @param ui
   * @param urlName
   * @return
   * @throws AuthorizationException
     */
  @PUT
  @Path("{urlName}")
  @Produces("text/plain")
  public Response updateUrl(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                            @PathParam("urlName") String urlName) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.PUT, createViewUrlResource(Optional.of(urlName)));
  }

  /**
   * Remove a view URL
   * @param body
   * @param headers
   * @param ui
   * @param urlName
   * @return
   * @throws AuthorizationException
     */
  @DELETE
  @Path("{urlName}")
  @Produces("text/plain")
  public Response deleteUrl(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                            @PathParam("urlName") String urlName) throws AuthorizationException {
    return handleRequest(headers, body, ui, Request.Type.DELETE, createViewUrlResource(Optional.of(urlName)));
  }


  /**
   * Get information about a single view URL
   * @param headers
   * @param ui
   * @param urlName
   * @return
   * @throws AuthorizationException
     */
  @GET
  @Path("{urlName}")
  @Produces("text/plain")
  public Response getUrl(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("urlName") String urlName) throws AuthorizationException {
    return handleRequest(headers, null, ui, Request.Type.GET, createViewUrlResource(Optional.of(urlName)));
  }




  // ----- helper methods ----------------------------------------------------

  /**
   * Create a view URL resource.
   *
   * @param urlName Name of the URL
   *
   * @return a view URL resource instance
   */
  private ResourceInstance createViewUrlResource(Optional<String> urlName) {
    return createResource(Resource.Type.ViewURL,Collections.singletonMap(Resource.Type.ViewURL, urlName.isPresent()?urlName.get().toString():null));
  }
}
