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
package org.apache.ambari.server.configuration.spring;

import org.apache.ambari.server.api.stomp.ApiStompAuthorizationService;
import org.apache.ambari.server.api.stomp.ApiStompEventProjector;
import org.apache.ambari.server.api.stomp.ApiStompInboundChannelInterceptor;
import org.apache.ambari.server.api.stomp.ApiStompOutboundChannelInterceptor;
import org.apache.ambari.server.api.stomp.ApiStompSessionRegistry;
import org.apache.ambari.server.api.stomp.NamedTasksSubscriptions;
import org.apache.ambari.server.api.stomp.TestController;
import org.apache.ambari.server.events.DefaultMessageEmitter;
import org.apache.ambari.server.events.listeners.requests.STOMPUpdateListener;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.state.Clusters;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;


@Configuration
@EnableWebSocketMessageBroker
@ComponentScan(basePackageClasses = {TestController.class})
@Import(RootStompConfig.class)
public class ApiStompConfig implements WebSocketMessageBrokerConfigurer {
  private final String HEARTBEAT_THREAD_NAME = "ws-heartbeat-thread-";
  private final int HEARTBEAT_POOL_SIZE = 1;
  private final org.apache.ambari.server.configuration.Configuration configuration;
  private final ApiStompSessionRegistry sessionRegistry;
  private final ApiStompAuthorizationService authorizationService;
  private final ApiStompEventProjector eventProjector;
  private final ApiStompInboundChannelInterceptor inboundChannelInterceptor;
  private final ApiStompOutboundChannelInterceptor outboundChannelInterceptor;
  private final HttpSessionHandshakeInterceptor httpSessionHandshakeInterceptor;

  public ApiStompConfig(Injector injector) {
    configuration = injector.getInstance(org.apache.ambari.server.configuration.Configuration.class);
    SessionHandler httpSessionHandler = injector.getInstance(SessionHandler.class);
    sessionRegistry = new ApiStompSessionRegistry(injector.getInstance(Users.class), httpSessionHandler);
    httpSessionHandler.addEventListener(sessionRegistry);
    authorizationService = new ApiStompAuthorizationService(
        injector.getInstance(Clusters.class),
        injector.getInstance(HostRoleCommandDAO.class),
        injector.getInstance(RequestDAO.class));
    eventProjector = new ApiStompEventProjector(authorizationService);
    inboundChannelInterceptor = new ApiStompInboundChannelInterceptor(sessionRegistry, authorizationService);
    outboundChannelInterceptor = new ApiStompOutboundChannelInterceptor(
        sessionRegistry, eventProjector, new ObjectMapper());
    httpSessionHandshakeInterceptor = new HttpSessionHandshakeInterceptor();
    httpSessionHandshakeInterceptor.setCopyAllAttributes(false);
    httpSessionHandshakeInterceptor.setCopyHttpSessionId(true);
    httpSessionHandshakeInterceptor.setCreateSession(true);
  }

  @Bean
  public STOMPUpdateListener requestSTOMPListener(Injector injector) {
    return new STOMPUpdateListener(injector, DefaultMessageEmitter.DEFAULT_API_EVENT_TYPES, true);
  }

  @Bean
  public ApiStompSessionRegistry apiStompSessionRegistry() {
    return sessionRegistry;
  }

  @Bean
  public ApiStompAuthorizationService apiStompAuthorizationService() {
    return authorizationService;
  }

  @Bean
  public ApiStompEventProjector apiStompEventProjector() {
    return eventProjector;
  }

  @Bean
  public NamedTasksSubscriptions namedTasksSubscribtions(Injector injector) {
    return injector.getInstance(NamedTasksSubscriptions.class);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(inboundChannelInterceptor);
  }

  @Override
  public void configureClientOutboundChannel(ChannelRegistration registration) {
    registration.interceptors(outboundChannelInterceptor);
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/v1")
      .setHandshakeHandler(getHandshakeHandler())
      // An empty allowlist activates Spring's same-origin policy for WebSocket and SockJS handshakes.
      .setAllowedOrigins()
      .addInterceptors(httpSessionHandshakeInterceptor)
      .withSockJS().setHeartbeatTime(configuration.getAPIHeartbeatInterval());
  }

  DefaultHandshakeHandler getHandshakeHandler() {
    return new DefaultHandshakeHandler(new JettyRequestUpgradeStrategy());
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(HEARTBEAT_POOL_SIZE);
    taskScheduler.setThreadNamePrefix(HEARTBEAT_THREAD_NAME);
    taskScheduler.initialize();

    registry.setPreservePublishOrder(true).enableSimpleBroker("/").setTaskScheduler(taskScheduler)
        .setHeartbeatValue(new long[]{
            configuration.getAPIHeartbeatInterval(), configuration.getAPIHeartbeatInterval()});
  }
}
