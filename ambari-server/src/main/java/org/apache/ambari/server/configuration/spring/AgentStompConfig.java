/**
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

import jakarta.servlet.ServletContext;

import org.apache.ambari.server.agent.stomp.HeartbeatController;
import org.apache.ambari.server.events.DefaultMessageEmitter;
import org.apache.ambari.server.events.listeners.requests.STOMPUpdateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.google.inject.Injector;

@Configuration
@EnableWebSocketMessageBroker
@ComponentScan(basePackageClasses = {HeartbeatController.class})
@Import({RootStompConfig.class,GuiceBeansConfig.class})
public class AgentStompConfig implements WebSocketMessageBrokerConfigurer {
  private org.apache.ambari.server.configuration.Configuration configuration;

  private final ServletContext servletContext;

  @Autowired
  private AgentRegisteringQueueChecker agentRegisteringQueueChecker;

  public AgentStompConfig(ServletContext servletContext, Injector injector) {
    this.servletContext = servletContext;
    configuration = injector.getInstance(org.apache.ambari.server.configuration.Configuration.class);
  }

  @Bean
  public STOMPUpdateListener requestSTOMPListener(Injector injector) {
    return new STOMPUpdateListener(injector, DefaultMessageEmitter.DEFAULT_AGENT_EVENT_TYPES);
  }

  public DefaultHandshakeHandler getHandshakeHandler() {
    return new DefaultHandshakeHandler(new JettyRequestUpgradeStrategy());
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/v1").setHandshakeHandler(getHandshakeHandler())
        .setAllowedOriginPatterns("*");

  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.taskExecutor().corePoolSize(configuration.getSpringMessagingThreadPoolSize());
  }

  @Override
  public void configureClientOutboundChannel(ChannelRegistration registration) {
    registration.taskExecutor().corePoolSize(configuration.getSpringMessagingThreadPoolSize());
//    registration.setInterceptors(agentRegisteringQueueChecker);
    registration.interceptors(agentRegisteringQueueChecker);
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.setMessageSizeLimit(configuration.getStompMaxIncomingMessageSize());
    registration.setSendBufferSizeLimit(configuration.getStompMaxBufferMessageSize());
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setPreservePublishOrder(true);
  }
}
