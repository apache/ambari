/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.conf;

import org.apache.ambari.logsearch.configurer.SslConfigurer;
import org.apache.ambari.logsearch.web.listener.LogSearchSessionListener;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.servlet.http.HttpSessionListener;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_APPLICATION_NAME;
import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_SESSION_ID;

@Configuration
public class LogSearchServletConfig {

  private static final Integer SESSION_TIMEOUT = 60 * 30;

  @Inject
  private ServerProperties serverProperties;

  @Inject
  private LogSearchHttpConfig logSearchHttpConfig;

  @Inject
  private SslConfigurer sslConfigurer;

  @Bean
  public HttpSessionListener httpSessionListener() {
    return new LogSearchSessionListener();
  }

  @Bean
  public ServletRegistrationBean jerseyServlet() {
    ServletRegistrationBean registration = new ServletRegistrationBean(new ServletContainer(), "/api/v1/*");
    registration.addInitParameter(ServletProperties.JAXRS_APPLICATION_CLASS, LogSearchJerseyResourceConfig.class.getName());
    return registration;
  }

  @Bean
  public EmbeddedServletContainerFactory containerFactory() {
    final JettyEmbeddedServletContainerFactory jettyEmbeddedServletContainerFactory = new JettyEmbeddedServletContainerFactory() {
      @Override
      protected JettyEmbeddedServletContainer getJettyEmbeddedServletContainer(Server server) {
        return new JettyEmbeddedServletContainer(server);
      }
    };
    jettyEmbeddedServletContainerFactory.setSessionTimeout(SESSION_TIMEOUT);
    serverProperties.getSession().getCookie().setName(LOGSEARCH_SESSION_ID);
    serverProperties.setDisplayName(LOGSEARCH_APPLICATION_NAME);
    if ("https".equals(logSearchHttpConfig.getProtocol())) {
      sslConfigurer.ensureStorePasswords();
      sslConfigurer.loadKeystore();
      jettyEmbeddedServletContainerFactory.addServerCustomizers((JettyServerCustomizer) server -> {
        SslContextFactory sslContextFactory = sslConfigurer.getSslContextFactory();
        ServerConnector sslConnector = new ServerConnector(server, sslContextFactory);
        sslConnector.setPort(logSearchHttpConfig.getHttpsPort());
        server.setConnectors(new Connector[]{sslConnector});
      });
    } else {
      jettyEmbeddedServletContainerFactory.setPort(logSearchHttpConfig.getHttpPort());
    }
    return jettyEmbeddedServletContainerFactory;
  }
}
