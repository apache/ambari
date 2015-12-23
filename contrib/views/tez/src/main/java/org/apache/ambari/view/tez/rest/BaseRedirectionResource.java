/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.tez.rest;

import org.apache.ambari.view.tez.exceptions.ProxyException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Base class for resources which will redirect the call to the active URL by fetching the current active URL.
 * Ex: To redirect a endpoint to active RM/ATS url.
 */
public abstract class BaseRedirectionResource {

  @Path("/{endpoint:.+}")
  @GET
  public Response getData(@Context UriInfo uriInfo, @PathParam("endpoint") String endpoint) {
    String url = getProxyUrl(endpoint, uriInfo.getQueryParameters());
    try {
      return Response.temporaryRedirect(new URI(url)).build();
    } catch (URISyntaxException e) {
      throw new ProxyException("Failed to set the redirection url to : " + url + ".Internal Error.",
        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage());
    }
  }

  public abstract String getProxyUrl(String endpoint, MultivaluedMap<String, String> queryParams);
}
