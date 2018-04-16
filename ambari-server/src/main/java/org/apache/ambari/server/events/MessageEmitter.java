/*
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
package org.apache.ambari.server.events;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotRegisteredException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Is used to define a strategy for emitting message to subscribers.
 */
public abstract class MessageEmitter {
  private final static Logger LOG = LoggerFactory.getLogger(MessageEmitter.class);
  protected final AgentSessionManager agentSessionManager;
  protected final SimpMessagingTemplate simpMessagingTemplate;


  public MessageEmitter(AgentSessionManager agentSessionManager, SimpMessagingTemplate simpMessagingTemplate) {
    this.agentSessionManager = agentSessionManager;
    this.simpMessagingTemplate = simpMessagingTemplate;
  }

  /**
   * Determines destinations and emits message.
   * @param event message should to be emitted.
   * @throws AmbariException
   */
  abstract void emitMessage(STOMPEvent event) throws AmbariException;

  /**
   * Creates STOMP message header.
   * @param sessionId
   * @return message header.
   */
  protected MessageHeaders createHeaders(String sessionId) {
    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    headerAccessor.setSessionId(sessionId);
    headerAccessor.setLeaveMutable(true);
    return headerAccessor.getMessageHeaders();
  }

  /**
   * Emits message to all subscribers.
   * @param event message should to be emitted.
   * @param destination
   */
  protected void emitMessageToAll(STOMPEvent event, String destination) {
    LOG.debug("Received status update event {}", event);
    simpMessagingTemplate.convertAndSend(destination, event);
  }

  /**
   * Emit message to specified host only.
   * @param event message should to be emitted.
   * @param destination
   * @throws HostNotRegisteredException in case host is not registered.
   */
  protected void emitMessageToHost(STOMPHostEvent event, String destination) throws HostNotRegisteredException {
    Long hostId = event.getHostId();
    String sessionId = agentSessionManager.getSessionId(hostId);
    LOG.debug("Received status update event {} for host {} registered with session ID {}", event, hostId, sessionId);
    MessageHeaders headers = createHeaders(sessionId);
    simpMessagingTemplate.convertAndSendToUser(sessionId, destination, event, headers);
  }
}
