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
package org.apache.ambari.server.agent.stomp;

import javax.ws.rs.WebApplicationException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.HeartBeat;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.HeartBeatResponse;
import org.apache.ambari.server.agent.Register;
import org.apache.ambari.server.agent.RegistrationResponse;
import org.apache.ambari.server.agent.RegistrationStatus;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.google.inject.Injector;

@Controller
@SendToUser("/")
@MessageMapping("/")
public class HeartbeatController {
  private static Log LOG = LogFactory.getLog(HeartbeatController.class);
  private final HeartBeatHandler hh;

  public HeartbeatController(Injector injector) {
    hh = injector.getInstance(HeartBeatHandler.class);
  }

  @SubscribeMapping("/register")
  public RegistrationResponse register(Register message)
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

  @SubscribeMapping("/heartbeat")
  public HeartBeatResponse heartbeat(HeartBeat message) {
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
