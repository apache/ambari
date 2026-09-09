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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Optional;

import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

public class NamedTasksSubscribeListenerTest {
  @Test
  public void recordsAuthorizedTaskAfterSessionRecheck() {
    NamedTasksSubscriptions subscriptions = createMock(NamedTasksSubscriptions.class);
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    expect(subscriptions.matchDestination("/events/tasks/41")).andReturn(Optional.of(41L));
    expect(sessions.currentAuthentication("session-a")).andReturn(Optional.of(user));
    expect(authorization.canViewTask(user, 41L, null)).andReturn(true);
    subscriptions.addTaskId("session-a", 41L, "subscription-a");
    replay(subscriptions, sessions, authorization);

    new NamedTasksSubscribeListener(subscriptions, sessions, authorization)
        .subscribe(subscribeEvent());

    verify(subscriptions, sessions, authorization);
  }

  @Test
  public void doesNotRecordTaskWhenPersistedTaskIsUnauthorized() {
    NamedTasksSubscriptions subscriptions = createMock(NamedTasksSubscriptions.class);
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    expect(subscriptions.matchDestination("/events/tasks/41")).andReturn(Optional.of(41L));
    expect(sessions.currentAuthentication("session-a")).andReturn(Optional.of(user));
    expect(authorization.canViewTask(user, 41L, null)).andReturn(false);
    replay(subscriptions, sessions, authorization);

    new NamedTasksSubscribeListener(subscriptions, sessions, authorization)
        .subscribe(subscribeEvent());

    verify(subscriptions, sessions, authorization);
  }

  private SessionSubscribeEvent subscribeEvent() {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
    accessor.setSessionId("session-a");
    accessor.setSubscriptionId("subscription-a");
    accessor.setDestination("/events/tasks/41");
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    return new SessionSubscribeEvent(this, message);
  }
}
