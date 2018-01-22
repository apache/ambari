/*
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
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

public class ServiceConfigVersionService extends BaseService {
  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Parent service group name.
   */
  private String m_serviceGroupName = null;

  /**
   * Parent service name.
   */
  private String m_serviceName = null;

  public ServiceConfigVersionService(String m_clusterName) {
    this.m_clusterName = m_clusterName;
  }

  /**
   * Constructor.
   *
   * @param clusterName cluster name
   * @param serviceGroupName service group name
   * @param serviceName service name
   */
  public ServiceConfigVersionService(String clusterName, String serviceGroupName, String serviceName) {
    m_clusterName = clusterName;
    m_serviceGroupName = serviceGroupName;
    m_serviceName = serviceName;
  }

  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createConfigurations(String body,@Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST, createServiceConfigResource(m_clusterName, m_serviceGroupName, m_serviceName));
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getServiceConfigVersions(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createServiceConfigResource(m_clusterName, m_serviceGroupName, m_serviceName));
  }

  ResourceInstance createServiceConfigResource(String clusterName, String serviceGroupName, String serviceName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.ServiceConfigVersion, null);
    if (serviceName != null && serviceGroupName != null) {
      mapIds.put(Resource.Type.ServiceGroup, serviceGroupName);
      mapIds.put(Resource.Type.Service, serviceName);
    }

    return createResource(Resource.Type.ServiceConfigVersion, mapIds);
  }
}
