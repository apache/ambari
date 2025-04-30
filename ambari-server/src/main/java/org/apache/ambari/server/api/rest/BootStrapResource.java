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

package org.apache.ambari.server.api.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.bootstrap.BSHostStatus;
import org.apache.ambari.server.bootstrap.BSResponse;
import org.apache.ambari.server.bootstrap.BootStrapImpl;
import org.apache.ambari.server.bootstrap.BootStrapStatus;
import org.apache.ambari.server.bootstrap.SshHostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("/bootstrap")
public class BootStrapResource {

  private static BootStrapImpl bsImpl;
  private static final Logger LOG = LoggerFactory.getLogger(BootStrapResource.class);

  @Inject
  public static void init(BootStrapImpl instance) {
    bsImpl = instance;
  }
  /**
   * Run bootstrap on a list of hosts.
   * @response.representation.200.doc
   *
   * @response.representation.200.mediaType application/json
   * @response.representation.406.doc Error in format
   * @response.representation.408.doc Request Timed out
   * @throws Exception
   */
  @POST @ApiIgnore // until documented
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public BSResponse bootStrap(SshHostInfo sshInfo, @Context UriInfo uriInfo) {
    
    normalizeHosts(sshInfo);

    BSResponse resp = bsImpl.runBootStrap(sshInfo);

    return resp;
  }

  /**
   * Current BootStrap Information thats running.
   * @response.representation.200.doc
   *
   * @response.representation.200.mediaType application/json
   * @response.representation.406.doc Error in format
   * @response.representation.408.doc Request Timed out
   * @throws Exception
   */
  @GET @ApiIgnore // until documented
  @Path("/{requestId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public BootStrapStatus getBootStrapStatus(@PathParam("requestId")
    long requestId, @Context UriInfo info) {
    BootStrapStatus status = bsImpl.getStatus(requestId);
    if (status == null)
      throw new WebApplicationException(Response.Status.NO_CONTENT);
    return status;
  }

  /**
   * Gets a list of bootstrapped hosts.
   *
   * @param uriInfo the host info, with no SSL key information
   */
  @GET @ApiIgnore // until documented
  @Path("/hosts")
  @Produces(MediaType.APPLICATION_JSON)
  public List<BSHostStatus> getBootStrapHosts(@Context UriInfo uriInfo) {
    List<BSHostStatus> allStatus = bsImpl.getHostInfo(null);

    if (0 == allStatus.size())
      throw new WebApplicationException(Response.Status.NO_CONTENT);

    return allStatus;
  }
  /**
   * Gets a list of bootstrapped hosts.
   *
   * @param info  the host info, with no SSL key information required
   */
  @POST @ApiIgnore // until documented
  @Path("/hosts")
  @Produces(MediaType.APPLICATION_JSON)
  public List<BSHostStatus> getBootStrapHosts(SshHostInfo info, @Context UriInfo uriInfo) {

    List<BSHostStatus> allStatus = bsImpl.getHostInfo(info.getHosts());

    if (0 == allStatus.size())
      throw new WebApplicationException(Response.Status.NO_CONTENT);

    return allStatus;
  }
  
  
  private void normalizeHosts(SshHostInfo info) {
    List<String> validHosts = new ArrayList<>();
    List<String> newHosts = new ArrayList<>();
    
    for (String host: info.getHosts()) {
      try {
        InetAddress addr = InetAddress.getByName(host);
        
        if (!validHosts.contains(addr.getHostAddress())) {
          validHosts.add(addr.getHostAddress());
          newHosts.add(host);
        } else {
          LOG.warn("Host " + host + " has already been targeted to be bootstrapped.");
        }
      } catch (UnknownHostException e) {
        LOG.warn("Host " + host + " cannot be determined.");
      }
    }
    
    info.setHosts(newHosts);
  }
  
}
