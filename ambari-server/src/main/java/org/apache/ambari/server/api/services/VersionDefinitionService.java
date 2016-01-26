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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

@Path("/version_definitions/")
public class VersionDefinitionService extends BaseService {

  @GET
  @Produces("text/plain")
  public Response getServices(@Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, null, ui, Request.Type.GET,
      createResource(null));
  }

  @GET
  @Path("{versionId}")
  @Produces("text/plain")
  public Response getService(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("versionId") Long versionId) {

    return handleRequest(headers, null, ui, Request.Type.GET,
      createResource(versionId));
  }

  @POST
  @Produces("text/plain")
  public Response createVersion(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
        createResource(null));
  }

  protected ResourceInstance createResource(Long versionId) {
    return createResource(Resource.Type.VersionDefinition,
        Collections.singletonMap(Resource.Type.VersionDefinition,
            null == versionId ? null : versionId.toString()));
  }

}
