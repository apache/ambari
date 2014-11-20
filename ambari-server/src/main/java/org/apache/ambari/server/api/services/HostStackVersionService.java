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
 * Service responsible for stack version requests.
 */
public class HostStackVersionService extends BaseService {
  /**
   * Name of the host.
   */
  private String hostName;

  /**
   * Constructor.
   *
   * @param hostName name of the host
   */
  public HostStackVersionService(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets all host host version.
   * Handles: GET /hosts/{hostname}/stack_versions requests.
   *
   * @param headers http headers
   * @param ui      uri info
   *
   * @return information regarding all host stack versions
   */
  @GET
  @Produces("text/plain")
  public Response getHostStackVersions(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(hostName, null));
  }

  /**
   * Gets a single host stack version.
   * Handles: GET /host/{hostname}/host_versions/{stackversionid} requests.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param stackversionid host stack version id
   *
   * @return information regarding the specific host stack version
   */
  @GET
  @Path("{stackversionid}")
  @Produces("text/plain")
  public Response getHostStackVersion(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("stackversionid") String stackVersionId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(hostName, stackVersionId));
  }

  /**
   * Create a host stack version resource instance.
   *
   * @param hostName host name
   * @param stackVersionId host stack version id
   * @return a host host version resource instance
   */
  private ResourceInstance createResource(String hostName, String stackVersionId) {
    final Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Host, hostName);
    mapIds.put(Resource.Type.HostStackVersion, stackVersionId);
    return createResource(Resource.Type.HostStackVersion, mapIds);
  }
}
