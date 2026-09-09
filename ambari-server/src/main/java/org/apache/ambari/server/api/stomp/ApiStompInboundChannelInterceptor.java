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
package org.apache.ambari.server.api.stomp;

import java.util.Optional;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

public class ApiStompInboundChannelInterceptor implements ChannelInterceptor {
  private final ApiStompSessionRegistry sessionRegistry;
  private final ApiStompAuthorizationService authorizationService;

  public ApiStompInboundChannelInterceptor(ApiStompSessionRegistry sessionRegistry,
                                           ApiStompAuthorizationService authorizationService) {
    this.sessionRegistry = sessionRegistry;
    this.authorizationService = authorizationService;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
    String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());

    if (messageType == SimpMessageType.CONNECT) {
      if (!sessionRegistry.register(
          sessionId,
          SimpMessageHeaderAccessor.getUser(message.getHeaders()),
          SimpMessageHeaderAccessor.getSessionAttributes(message.getHeaders())).isPresent()) {
        throw new AccessDeniedException("An authenticated Ambari user is required for API event connections");
      }
      return message;
    }

    if (messageType == SimpMessageType.DISCONNECT) {
      sessionRegistry.remove(sessionId);
      return message;
    }
    if (messageType == SimpMessageType.UNSUBSCRIBE || messageType == SimpMessageType.HEARTBEAT) {
      return message;
    }
    if (messageType == SimpMessageType.MESSAGE) {
      throw new AccessDeniedException("Client messages to API event destinations are not allowed");
    }
    if (messageType != SimpMessageType.SUBSCRIBE) {
      throw new AccessDeniedException("Unsupported API event message type");
    }

    Optional<Authentication> authentication = sessionRegistry.currentAuthentication(sessionId);
    if (!authentication.isPresent()
        || !sessionRegistry.principalMatches(sessionId, SimpMessageHeaderAccessor.getUser(message.getHeaders()))) {
      throw new AccessDeniedException("The API event session is no longer authenticated");
    }

    String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
    if (!ApiStompDestinations.isAllowedSubscription(destination)) {
      throw new AccessDeniedException("Unknown or non-exact API event subscription destination");
    }

    Optional<Long> taskId = ApiStompDestinations.taskId(destination);
    if (taskId.isPresent() && !authorizationService.canViewTask(authentication.get(), taskId.get(), null)) {
      throw new AccessDeniedException("The authenticated user cannot view the requested task");
    }
    return message;
  }
}
