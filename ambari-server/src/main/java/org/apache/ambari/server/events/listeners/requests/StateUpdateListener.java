
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events.listeners.requests;

import org.apache.ambari.server.HostNotRegisteredException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.events.AmbariHostUpdateEvent;
import org.apache.ambari.server.events.AmbariUpdateEvent;
import org.apache.ambari.server.events.publishers.StateUpdateEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;

public class StateUpdateListener {

  private final static Logger LOG = LoggerFactory.getLogger(StateUpdateListener.class);

  private final AgentSessionManager agentSessionManager;

  @Autowired
  SimpMessagingTemplate simpMessagingTemplate;

  public StateUpdateListener(Injector injector) {
    StateUpdateEventPublisher stateUpdateEventPublisher =
      injector.getInstance(StateUpdateEventPublisher.class);
    agentSessionManager = injector.getInstance(AgentSessionManager.class);
    stateUpdateEventPublisher.register(this);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onUpdateEvent(AmbariUpdateEvent event) throws HostNotRegisteredException {
    if (event instanceof AmbariHostUpdateEvent) {
      AmbariHostUpdateEvent ambariHostUpdateEvent = (AmbariHostUpdateEvent) event;
      String sessionId = agentSessionManager.getSessionId(ambariHostUpdateEvent.getHostName());
      LOG.debug("Received status update event {} for host ()", ambariHostUpdateEvent.toString(),
          ambariHostUpdateEvent.getHostName());
      simpMessagingTemplate.convertAndSendToUser(sessionId, ambariHostUpdateEvent.getDestination(),
          ambariHostUpdateEvent, createHeaders(sessionId));
    } else {
      LOG.debug("Received status update event {}", event.toString());
      simpMessagingTemplate.convertAndSend(event.getDestination(), event);
    }
  }

  private MessageHeaders createHeaders(String sessionId) {
    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    headerAccessor.setSessionId(sessionId);
    headerAccessor.setLeaveMutable(true);
    return headerAccessor.getMessageHeaders();
  }
}
