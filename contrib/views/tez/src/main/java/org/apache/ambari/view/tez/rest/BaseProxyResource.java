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

import com.google.inject.Inject;
import org.apache.ambari.view.tez.exceptions.ProxyException;
import org.apache.ambari.view.tez.utils.ProxyHelper;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;

/**
 * Base class for the proxy resources
 */
public abstract class BaseProxyResource {

  private ProxyHelper proxyHelper;

  @Inject
  public BaseProxyResource(ProxyHelper proxyHelper) {
    this.proxyHelper = proxyHelper;
  }

  @Path("/{endpoint:.+}")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public Response getData(@Context UriInfo uriInfo, @PathParam("endpoint") String endpoint) {
    String url = getProxyUrl(endpoint, uriInfo.getQueryParameters());
    String response = proxyHelper.getResponse(url, new HashMap<String, String>());

    JSONObject jsonObject = (JSONObject) JSONValue.parse(response);

    if (jsonObject == null) {
      throw new ProxyException("Failed to parse JSON from URL : " + url + ".Internal Error.",
        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response);
    }
    return Response.ok(jsonObject).type(MediaType.APPLICATION_JSON).build();
  }

  public abstract String getProxyUrl(String endpoint, MultivaluedMap<String, String> queryParams);
}
