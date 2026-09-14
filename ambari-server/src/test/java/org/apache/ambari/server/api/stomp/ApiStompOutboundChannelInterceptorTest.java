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

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNull;

import java.util.Optional;

import org.apache.ambari.server.events.AlertUpdateEvent;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiStompOutboundChannelInterceptorTest {
  @Test
  public void missingSessionDropsEvent() {
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompEventProjector projector = createMock(ApiStompEventProjector.class);
    expect(sessions.currentAuthentication(null)).andReturn(Optional.empty());
    replay(sessions, projector);

    Message<?> result = new ApiStompOutboundChannelInterceptor(sessions, projector, new ObjectMapper())
        .preSend(eventMessage(null, true), null);

    assertNull(result);
  }

  @Test
  public void authenticationReloadFailureDropsEvent() {
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompEventProjector projector = createMock(ApiStompEventProjector.class);
    expect(sessions.currentAuthentication("session-a")).andThrow(new IllegalStateException("sensitive-value"));
    replay(sessions, projector);

    Message<?> result = new ApiStompOutboundChannelInterceptor(sessions, projector, new ObjectMapper())
        .preSend(eventMessage("session-a", true), null);

    assertNull(result);
  }

  @Test
  public void missingTrustedEventHeaderDropsEventBeforeAuthorization() {
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompEventProjector projector = createMock(ApiStompEventProjector.class);
    replay(sessions, projector);

    Message<?> result = new ApiStompOutboundChannelInterceptor(sessions, projector, new ObjectMapper())
        .preSend(eventMessage("session-a", false), null);

    assertNull(result);
  }

  private Message<?> eventMessage(String sessionId, boolean includeTrustedEvent) {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    accessor.setSessionId(sessionId);
    accessor.setDestination("/events/alerts");
    if (includeTrustedEvent) {
      accessor.setHeader(ApiStompEventMessageHeaders.ORIGINAL_EVENT, new AlertUpdateEvent(emptyMap()));
    }
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
