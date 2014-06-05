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

package org.apache.ambari.view.weather;

import org.apache.ambari.view.ViewResourceHandler;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Service for the city resource of the weather view.
 */
public class CityService {
  /**
   * The resource request handler.
   */
  @Inject
  ViewResourceHandler resourceHandler;

  /**
   * Handles: GET /cities/{cityName} Get a specific city.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param cityName  city name
   *
   * @return city instance representation
   */
  @GET
  @Path("{cityName}")
  @Produces({"application/json"})
  public Response getCity(@Context HttpHeaders headers, @Context UriInfo ui,
                          @PathParam("cityName") String cityName) {
    return resourceHandler.handleRequest(headers, ui, ViewResourceHandler.RequestType.GET,
        ViewResourceHandler.MediaType.APPLICATION_JSON, cityName);
  }

  /**
   * Handles: GET /cities Get all cities.
   *
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @return city collection resource representation
   */
  @GET
  @Produces({"text/plain", "application/json"})
  public Response getCities(@Context HttpHeaders headers, @Context UriInfo ui) {
    return resourceHandler.handleRequest(headers, ui, null);
  }
}
