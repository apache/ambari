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
 * Service responsible for cluster stack version requests.
 */
public class ClusterStackVersionService extends BaseService {
  /**
   * Name of the cluster.
   */
  private String clusterName;

  /**
   * Constructor.
   *
   * @param clusterName name of the cluster
   */
  public ClusterStackVersionService(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Gets all cluster stack version.
   * Handles: GET /clusters/{clustername}/stack_versions requests.
   *
   * @param headers http headers
   * @param ui      uri info
   *
   * @return information regarding all cluster stack versions
   */
  @GET
  @Produces("text/plain")
  public Response getClusterStackVersions(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(clusterName, null));
  }

  /**
   * Gets a single cluster stack version.
   * Handles: GET /clusters/{clustername}/stack_versions/{stackversionid} requests.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param stackVersionId cluster stack version id
   *
   * @return information regarding the specific cluster stack version
   */
  @GET
  @Path("{stackversionid}")
  @Produces("text/plain")
  public Response getClusterStackVersion(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("stackversionid") String stackVersionId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(clusterName, stackVersionId));
  }

  /**
   * Create a cluster stack version resource instance.
   *
   * @param clusterName    cluster name
   * @param stackVersionId cluster stack version id
   *
   * @return a cluster stack version resource instance
   */
  private ResourceInstance createResource(String clusterName, String stackVersionId) {
    final Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.ClusterStackVersion, stackVersionId);
    return createResource(Resource.Type.ClusterStackVersion, mapIds);
  }
}
