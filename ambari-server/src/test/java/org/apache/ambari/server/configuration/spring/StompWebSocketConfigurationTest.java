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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletContext;

import org.apache.ambari.server.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.google.inject.Injector;

public class StompWebSocketConfigurationTest {
  private Injector injector;
  private ServletContext servletContext;

  @Before
  public void setUp() {
    injector = mock(Injector.class);
    servletContext = mock(ServletContext.class);
    when(injector.getInstance(Configuration.class)).thenReturn(mock(Configuration.class));
  }

  @Test
  public void usesJetty11UpgradeStrategyForEveryStompEndpoint() {
    AgentStompConfig agentConfig = new AgentStompConfig(servletContext, injector);
    ApiStompConfig apiConfig = new ApiStompConfig(injector);
    RootStompConfig rootConfig = new RootStompConfig(servletContext, injector);

    assertJetty11UpgradeStrategy(agentConfig.getHandshakeHandler());
    assertJetty11UpgradeStrategy(apiConfig.getHandshakeHandler());
    assertJetty11UpgradeStrategy(rootConfig.handshakeHandler());
  }

  private void assertJetty11UpgradeStrategy(DefaultHandshakeHandler handshakeHandler) {
    assertTrue(handshakeHandler.getRequestUpgradeStrategy() instanceof Jetty11RequestUpgradeStrategy);
  }
}
