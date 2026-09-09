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
import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
import org.apache.ambari.server.api.stomp.ApiStompAuthorizationService.EventAccess;
import org.apache.ambari.server.events.AlertUpdateEvent;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiStompBrokerIsolationTest {
  @Test
  public void simpleBrokerDeliversDifferentProjectedPayloadPerSession() throws Exception {
    Authentication clusterA = TestAuthenticationFactory.createClusterUser("alice", 900L);
    Authentication clusterB = TestAuthenticationFactory.createClusterUser("bob", 901L);
    ApiStompSessionRegistry sessions = createMock(ApiStompSessionRegistry.class);
    expect(sessions.currentAuthentication("session-a")).andReturn(Optional.of(clusterA));
    expect(sessions.currentAuthentication("session-b")).andReturn(Optional.of(clusterB));
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    expect(authorization.isAuthorized(clusterA, 101L, EventAccess.ALERT)).andReturn(true);
    expect(authorization.isAuthorized(clusterA, 202L, EventAccess.ALERT)).andReturn(false);
    expect(authorization.isAuthorized(clusterB, 101L, EventAccess.ALERT)).andReturn(false);
    expect(authorization.isAuthorized(clusterB, 202L, EventAccess.ALERT)).andReturn(true);
    replay(sessions, authorization);

    ObjectMapper mapper = new ObjectMapper();
    ExecutorSubscribableChannel inbound = new ExecutorSubscribableChannel();
    ExecutorSubscribableChannel outbound = new ExecutorSubscribableChannel();
    ExecutorSubscribableChannel brokerChannel = new ExecutorSubscribableChannel();
    outbound.addInterceptor(new ApiStompOutboundChannelInterceptor(
        sessions, new ApiStompEventProjector(authorization), mapper));
    List<Message<?>> deliveries = new ArrayList<>();
    outbound.subscribe(deliveries::add);

    SimpleBrokerMessageHandler broker = new SimpleBrokerMessageHandler(
        inbound, outbound, brokerChannel, singletonList("/events"));
    broker.start();
    try {
      inbound.send(subscription("session-a", "subscription-a"));
      inbound.send(subscription("session-b", "subscription-b"));

      Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> summaries = new HashMap<>();
      summaries.put(101L, emptyMap());
      summaries.put(202L, emptyMap());
      AlertUpdateEvent event = new AlertUpdateEvent(summaries);
      SimpMessagingTemplate template = new SimpMessagingTemplate(brokerChannel);
      template.setMessageConverter(new MappingJackson2MessageConverter());
      ApiStompEventMessageHeaders.convertAndSend(template, "/events/alerts", event);

      assertEquals(2, deliveries.size());
      Map<String, JsonNode> payloadBySession = new HashMap<>();
      for (Message<?> delivery : deliveries) {
        payloadBySession.put(SimpMessageHeaderAccessor.getSessionId(delivery.getHeaders()),
            mapper.readTree((byte[]) delivery.getPayload()));
        assertFalse(delivery.getHeaders().containsKey(ApiStompEventMessageHeaders.ORIGINAL_EVENT));
      }
      assertTrue(payloadBySession.get("session-a").path("summaries").has("101"));
      assertFalse(payloadBySession.get("session-a").path("summaries").has("202"));
      assertTrue(payloadBySession.get("session-b").path("summaries").has("202"));
      assertFalse(payloadBySession.get("session-b").path("summaries").has("101"));
      assertEquals(2, event.getSummaries().size());
    } finally {
      broker.stop();
    }
  }

  private Message<?> subscription(String sessionId, String subscriptionId) {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
    accessor.setSessionId(sessionId);
    accessor.setSubscriptionId(subscriptionId);
    accessor.setDestination("/events/alerts");
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

}
