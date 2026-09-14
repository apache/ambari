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

import static java.util.Collections.singletonMap;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertSame;

import java.util.Map;
import java.util.Optional;

import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

public class ApiStompInboundChannelInterceptorTest {
  @Test
  public void registersConnectUsingServerPrincipalAndHttpSession() {
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    Map<String, Object> attributes = singletonMap(
        HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME, "http-a");
    expect(sessions.register("session-a", user, attributes)).andReturn(Optional.of(user));
    replay(sessions, authorization);

    Message<?> connect = message(SimpMessageType.CONNECT, "session-a", null, user, attributes);

    assertSame(connect, new ApiStompInboundChannelInterceptor(sessions, authorization)
        .preSend(connect, null));
  }

  @Test(expected = AccessDeniedException.class)
  public void rejectsConnectWithoutTrustedAmbariIdentity() {
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    expect(sessions.register("session-a", null, null)).andReturn(Optional.empty());
    replay(sessions, authorization);

    new ApiStompInboundChannelInterceptor(sessions, authorization)
        .preSend(message(SimpMessageType.CONNECT, "session-a", null), null);
  }

  @Test
  public void acceptsExactLegacySubscriptionForCurrentSession() {
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    expect(sessions.currentAuthentication("session-a")).andReturn(Optional.of(user));
    expect(sessions.principalMatches("session-a", null)).andReturn(true);
    replay(sessions, authorization);

    Message<?> subscribe = message(SimpMessageType.SUBSCRIBE, "session-a", "/events/alerts");
    ApiStompInboundChannelInterceptor interceptor =
        new ApiStompInboundChannelInterceptor(sessions, authorization);

    assertSame(subscribe, interceptor.preSend(subscribe, null));
  }

  @Test(expected = AccessDeniedException.class)
  public void rejectsWildcardSubscription() {
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    expect(sessions.currentAuthentication("session-a")).andReturn(Optional.of(user));
    expect(sessions.principalMatches("session-a", null)).andReturn(true);
    replay(sessions, authorization);

    new ApiStompInboundChannelInterceptor(sessions, authorization)
        .preSend(message(SimpMessageType.SUBSCRIBE, "session-a", "/events/**"), null);
  }

  @Test(expected = AccessDeniedException.class)
  public void rejectsClientSend() {
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    replay(sessions, authorization);

    new ApiStompInboundChannelInterceptor(sessions, authorization)
        .preSend(message(SimpMessageType.MESSAGE, "session-a", "/events/alerts"), null);
  }

  @Test(expected = AccessDeniedException.class)
  public void rejectsForeignNamedTaskSubscription() {
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    expect(sessions.currentAuthentication("session-a")).andReturn(Optional.of(user));
    expect(sessions.principalMatches("session-a", null)).andReturn(true);
    expect(authorization.canViewTask(user, 41L, null)).andReturn(false);
    replay(sessions, authorization);

    new ApiStompInboundChannelInterceptor(sessions, authorization)
        .preSend(message(SimpMessageType.SUBSCRIBE, "session-a", "/events/tasks/41"), null);
  }

  private Message<?> message(SimpMessageType type, String sessionId, String destination) {
    return message(type, sessionId, destination, null, null);
  }

  private Message<?> message(SimpMessageType type, String sessionId, String destination,
                             Authentication user, Map<String, Object> attributes) {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(type);
    accessor.setSessionId(sessionId);
    accessor.setDestination(destination);
    if (destination != null) {
      accessor.setSubscriptionId("subscription-1");
    }
    accessor.setUser(user);
    accessor.setSessionAttributes(attributes);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
