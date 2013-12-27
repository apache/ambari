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

package org.apache.ambari.server.agent.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.HeartBeat;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.HeartBeatResponse;
import org.apache.ambari.server.agent.Register;
import org.apache.ambari.server.agent.RegistrationResponse;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import org.apache.ambari.server.agent.RegistrationStatus;

/**
 * Agent Resource represents Ambari agent controller.
 * It provides API for Ambari agents to get the cluster configuration changes
 * as well as report the node attributes and state of services running the on
 * the cluster nodes
 */
@Path("/")
public class AgentResource {
  private static HeartBeatHandler hh;
  private static Log LOG = LogFactory.getLog(AgentResource.class);

  @Inject
  public static void init(HeartBeatHandler instance) {
    hh = instance;
    hh.start();
  }

  /**
   * Register information about the host (Internal API to be used for
   * Ambari Agent)
   * @response.representation.200.doc This API is invoked by Ambari agent running
   *  on a cluster to register with the server.
   * @response.representation.200.mediaType application/json
   * @response.representation.406.doc Error in register message format
   * @response.representation.408.doc Request Timed out
   * @param message Register message
   * @throws InvalidStateTransitionException
   * @throws AmbariException
   * @throws Exception
   */
  @Path("register/{hostName}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON})
  public RegistrationResponse register(Register message,
      @Context HttpServletRequest req)
      throws WebApplicationException, InvalidStateTransitionException {
    /* Call into the heartbeat handler */

    RegistrationResponse response = null;
    try {
      response = hh.handleRegistration(message);
      LOG.debug("Sending registration response " + response);
    } catch (AmbariException ex) {
      response = new RegistrationResponse();
      response.setResponseId(-1);
      response.setResponseStatus(RegistrationStatus.FAILED);
      response.setExitstatus(1);
      response.setLog(ex.getMessage());
      return response;
    }
    return response;
  }

  /**
   * Update state of the node (Internal API to be used by Ambari agent).
   *
   * @response.representation.200.doc This API is invoked by Ambari agent running
   *  on a cluster to update the state of various services running on the node.
   * @response.representation.200.mediaType application/json
   * @response.representation.406.doc Error in heartbeat message format
   * @response.representation.408.doc Request Timed out
   * @param message Heartbeat message
   * @throws Exception
   */
  @Path("heartbeat/{hostName}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON})
  public HeartBeatResponse heartbeat(HeartBeat message)
      throws WebApplicationException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Received Heartbeat message " + message);
    }
    HeartBeatResponse heartBeatResponse;
    try {
      heartBeatResponse = hh.handleHeartBeat(message);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Sending heartbeat response with response id " + heartBeatResponse.getResponseId());
        LOG.debug("Response details " + heartBeatResponse);
      }
    } catch (Exception e) {
      LOG.warn("Error in HeartBeat", e);
      throw new WebApplicationException(500);
    }
    return heartBeatResponse;
  }
}
