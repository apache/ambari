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

import org.apache.ambari.server.events.STOMPEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiStompOutboundChannelInterceptor implements ChannelInterceptor {
  private static final Logger LOG = LoggerFactory.getLogger(ApiStompOutboundChannelInterceptor.class);

  private final ApiStompSessionRegistry sessionRegistry;
  private final ApiStompEventProjector eventProjector;
  private final ObjectMapper objectMapper;

  public ApiStompOutboundChannelInterceptor(ApiStompSessionRegistry sessionRegistry,
                                            ApiStompEventProjector eventProjector,
                                            ObjectMapper objectMapper) {
    this.sessionRegistry = sessionRegistry;
    this.eventProjector = eventProjector;
    this.objectMapper = objectMapper;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) {
      return message;
    }

    String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
    if (!ApiStompDestinations.isEventDestination(destination)) {
      return message;
    }

    String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
    Object originalEvent = message.getHeaders().get(ApiStompEventMessageHeaders.ORIGINAL_EVENT);
    if (!(originalEvent instanceof STOMPEvent)) {
      return null;
    }

    try {
      Optional<Authentication> authentication = sessionRegistry.currentAuthentication(sessionId);
      if (!authentication.isPresent()) {
        return null;
      }
      Optional<STOMPEvent> projected = eventProjector.project(
          (STOMPEvent) originalEvent, destination, authentication.get());
      if (!projected.isPresent()) {
        return null;
      }

      byte[] payload = objectMapper.writeValueAsBytes(projected.get());
      SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
      accessor.setHeader(ApiStompEventMessageHeaders.ORIGINAL_EVENT, null);
      accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
      return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    } catch (JsonProcessingException | RuntimeException e) {
      LOG.warn("Dropping an API event that could not be safely projected for destination {}; cause={}",
          destination, e.getClass().getSimpleName());
      return null;
    }
  }
}
