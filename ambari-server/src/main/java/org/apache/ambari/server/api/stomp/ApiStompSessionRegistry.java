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

import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.ambari.server.security.authentication.AmbariUserDetails;
import org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl;
import org.apache.ambari.server.security.authorization.Users;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.session.ManagedSession;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

public class ApiStompSessionRegistry implements HttpSessionListener {
  private final Users users;
  private final SessionHandler httpSessionHandler;
  private final ConcurrentHashMap<String, SessionIdentity> sessions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Set<String>> sessionsByHttpSession = new ConcurrentHashMap<>();

  public ApiStompSessionRegistry(Users users, SessionHandler httpSessionHandler) {
    this.users = users;
    this.httpSessionHandler = httpSessionHandler;
  }

  public Optional<Authentication> register(String sessionId, Principal principal,
                                           Map<String, Object> sessionAttributes) {
    Object httpSessionAttribute = sessionAttributes == null ? null : sessionAttributes.get(
        HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME);
    String httpSessionId = httpSessionAttribute instanceof String ? (String) httpSessionAttribute : null;
    SessionIdentity identity = identityFromPrincipal(principal, httpSessionId);
    if (sessionId == null || identity == null || !isHttpSessionActive(httpSessionId)) {
      return Optional.empty();
    }

    SessionIdentity existing = sessions.putIfAbsent(sessionId, identity);
    if (existing != null && !existing.equals(identity)) {
      return Optional.empty();
    }
    sessionsByHttpSession.computeIfAbsent(
        httpSessionId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
    Optional<Authentication> authentication = currentAuthentication(sessionId);
    if (!authentication.isPresent()) {
      remove(sessionId);
    }
    return authentication;
  }

  public Optional<Authentication> currentAuthentication(String sessionId) {
    if (sessionId == null) {
      return Optional.empty();
    }
    SessionIdentity identity = sessions.get(sessionId);
    if (identity == null) {
      return Optional.empty();
    }

    try {
      if (!isHttpSessionActive(identity.httpSessionId)) {
        remove(sessionId);
        return Optional.empty();
      }
      UserEntity user = users.getUserEntity(identity.userId);
      if (!isActive(user) || !identity.userName.equals(user.getUserName())) {
        remove(sessionId);
        return Optional.empty();
      }

      AmbariUserDetailsImpl details = new AmbariUserDetailsImpl(
          users.getUser(user), null, users.getUserAuthorities(user));
      return Optional.of(new AmbariUserAuthentication(null, details, true));
    } catch (RuntimeException e) {
      remove(sessionId);
      return Optional.empty();
    }
  }

  public boolean principalMatches(String sessionId, Principal principal) {
    if (principal == null) {
      return true;
    }
    SessionIdentity identity = sessions.get(sessionId);
    SessionIdentity principalIdentity = identityFromPrincipal(principal,
        identity == null ? null : identity.httpSessionId);
    return identity != null && identity.equals(principalIdentity);
  }

  public void remove(String sessionId) {
    if (sessionId != null) {
      SessionIdentity removed = sessions.remove(sessionId);
      if (removed != null) {
        sessionsByHttpSession.computeIfPresent(removed.httpSessionId, (id, websocketSessions) -> {
          websocketSessions.remove(sessionId);
          return websocketSessions.isEmpty() ? null : websocketSessions;
        });
      }
    }
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent event) {
    removeHttpSession(event.getSession().getId());
  }

  void removeHttpSession(String httpSessionId) {
    Set<String> websocketSessions = sessionsByHttpSession.remove(httpSessionId);
    if (websocketSessions != null) {
      for (String websocketSession : websocketSessions) {
        sessions.remove(websocketSession);
      }
    }
  }

  boolean contains(String sessionId) {
    return sessions.containsKey(sessionId);
  }

  private boolean isActive(UserEntity user) {
    return user != null && user.getUserId() != null && user.getUserName() != null
        && Boolean.TRUE.equals(user.getActive());
  }

  private boolean isHttpSessionActive(String httpSessionId) {
    if (httpSessionId == null) {
      return false;
    }
    ManagedSession session = null;
    try {
      session = httpSessionHandler.getManagedSession(httpSessionId);
      return session != null && session.isValid() && !session.isExpiredAt(System.currentTimeMillis());
    } catch (RuntimeException e) {
      return false;
    } finally {
      if (session != null) {
        httpSessionHandler.complete(session);
      }
    }
  }

  private SessionIdentity identityFromPrincipal(Principal principal, String httpSessionId) {
    if (!(principal instanceof Authentication)) {
      return null;
    }
    Authentication authentication = (Authentication) principal;
    if (!authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AmbariUserDetails)) {
      return null;
    }

    AmbariUserDetails details = (AmbariUserDetails) authentication.getPrincipal();
    if (details.getUserId() == null || details.getUsername() == null
        || !details.getUsername().equals(authentication.getName())) {
      return null;
    }
    try {
      UserEntity user = users.getUserEntity(details.getUserId());
      if (!isActive(user) || !details.getUserId().equals(user.getUserId())
          || !details.getUsername().equals(user.getUserName())) {
        return null;
      }
      return new SessionIdentity(user.getUserId(), user.getUserName(), httpSessionId);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static final class SessionIdentity {
    private final Integer userId;
    private final String userName;
    private final String httpSessionId;

    private SessionIdentity(Integer userId, String userName, String httpSessionId) {
      this.userId = userId;
      this.userName = userName;
      this.httpSessionId = httpSessionId;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (!(object instanceof SessionIdentity)) {
        return false;
      }
      SessionIdentity that = (SessionIdentity) object;
      return Objects.equals(userId, that.userId) && Objects.equals(userName, that.userName)
          && Objects.equals(httpSessionId, that.httpSessionId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, userName, httpSessionId);
    }
  }
}
