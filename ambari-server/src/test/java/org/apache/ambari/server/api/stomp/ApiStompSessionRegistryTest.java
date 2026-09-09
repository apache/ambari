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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;

import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.session.ManagedSession;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

public class ApiStompSessionRegistryTest {
  @Test
  public void reloadsUserAndRemovesRevokedSession() {
    Users users = createMock(Users.class);
    UserEntity entity = user(1, "alice");
    expect(users.getUserEntity(1)).andReturn(entity).times(3);
    expect(users.getUser(entity)).andReturn(new User(entity));
    expect(users.getUserAuthorities(entity)).andReturn(emptyList());
    SessionHandler httpSessions = activeHttpSession("http-a", 3);
    replay(users);

    ApiStompSessionRegistry registry = new ApiStompSessionRegistry(users, httpSessions);
    Authentication principal = TestAuthenticationFactory.createNoRoleUser("alice", 100L);

    Authentication current = registry.register("session-a", principal, attributes("http-a")).get();
    assertTrue(current.isAuthenticated());
    assertNull(current.getCredentials());

    entity.setActive(false);
    assertFalse(registry.currentAuthentication("session-a").isPresent());
    assertFalse(registry.contains("session-a"));
    verify(users, httpSessions);
  }

  @Test
  public void rejectsNameOnlyAndStaleSameNamePrincipal() {
    Users users = createMock(Users.class);
    UserEntity original = user(1, "alice");
    expect(users.getUserEntity(1)).andReturn(original).times(2).andReturn(user(2, "alice"));
    expect(users.getUser(original)).andReturn(new User(original));
    expect(users.getUserAuthorities(original)).andReturn(emptyList());
    SessionHandler httpSessions = activeHttpSession("http-a", 2);
    replay(users);

    ApiStompSessionRegistry registry = new ApiStompSessionRegistry(users, httpSessions);
    UsernamePasswordAuthenticationToken nameOnly =
        new UsernamePasswordAuthenticationToken("alice", null, emptyList());
    assertFalse(registry.register("name-only", nameOnly, attributes("http-a")).isPresent());

    Authentication oldPrincipal = TestAuthenticationFactory.createNoRoleUser("alice", 100L);
    assertTrue(registry.register("original", oldPrincipal, attributes("http-a")).isPresent());
    assertFalse(registry.register("recreated", oldPrincipal, attributes("http-a")).isPresent());
    verify(users, httpSessions);
  }

  @Test
  public void invalidatingHttpSessionRemovesAllWebSocketSessions() {
    Users users = createMock(Users.class);
    UserEntity entity = user(1, "alice");
    expect(users.getUserEntity(1)).andReturn(entity).times(2);
    expect(users.getUser(entity)).andReturn(new User(entity));
    expect(users.getUserAuthorities(entity)).andReturn(emptyList());
    SessionHandler httpSessions = activeHttpSession("http-a", 2);
    HttpSession destroyed = createMock(HttpSession.class);
    expect(destroyed.getId()).andReturn("http-a");
    replay(users, destroyed);

    ApiStompSessionRegistry registry = new ApiStompSessionRegistry(users, httpSessions);
    assertTrue(registry.register("session-a", TestAuthenticationFactory.createNoRoleUser("alice", 100L),
        attributes("http-a")).isPresent());

    registry.sessionDestroyed(new HttpSessionEvent(destroyed));
    assertFalse(registry.currentAuthentication("session-a").isPresent());
    verify(users, httpSessions, destroyed);
  }

  @Test
  public void expiredHttpSessionFailsClosed() {
    Users users = createMock(Users.class);
    UserEntity entity = user(1, "alice");
    expect(users.getUserEntity(1)).andReturn(entity).times(2);
    expect(users.getUser(entity)).andReturn(new User(entity));
    expect(users.getUserAuthorities(entity)).andReturn(emptyList());
    SessionHandler handler = createMock(SessionHandler.class);
    ManagedSession session = createMock(ManagedSession.class);
    expect(handler.getManagedSession("http-a")).andReturn(session).times(3);
    expect(session.isValid()).andReturn(true).times(2).andReturn(false);
    expect(session.isExpiredAt(org.easymock.EasyMock.anyLong())).andReturn(false).times(2);
    handler.complete(session);
    expectLastCall().times(3);
    replay(users, handler, session);

    ApiStompSessionRegistry registry = new ApiStompSessionRegistry(users, handler);
    assertTrue(registry.register("session-a", TestAuthenticationFactory.createNoRoleUser("alice", 100L),
        attributes("http-a")).isPresent());
    assertFalse(registry.currentAuthentication("session-a").isPresent());
    verify(users, handler, session);
  }

  private SessionHandler activeHttpSession(String id, int checks) {
    SessionHandler handler = createMock(SessionHandler.class);
    ManagedSession session = createMock(ManagedSession.class);
    expect(handler.getManagedSession(id)).andReturn(session).times(checks);
    expect(session.isValid()).andReturn(true).times(checks);
    expect(session.isExpiredAt(org.easymock.EasyMock.anyLong())).andReturn(false).times(checks);
    handler.complete(session);
    expectLastCall().times(checks);
    replay(handler, session);
    return handler;
  }

  private Map<String, Object> attributes(String httpSessionId) {
    return singletonMap(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME, httpSessionId);
  }

  private UserEntity user(int id, String name) {
    UserEntity entity = new UserEntity();
    entity.setUserId(id);
    entity.setUserName(name);
    entity.setActive(true);
    return entity;
  }
}
