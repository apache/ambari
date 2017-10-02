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
package org.apache.ambari.infra.conf;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;

@Configuration
public class InfraManagerServletConfig {

  private static final Integer SESSION_TIMEOUT = 60 * 30;
  private static final String INFRA_MANAGER_SESSIONID = "INFRAMANAGER_SESSIONID";
  private static final String INFRA_MANAGER_APPLICATION_NAME = "infra-manager";

  @Value("${infra-manager.server.port:61890}")
  private int port;

  @Inject
  private ServerProperties serverProperties;

  @Inject
  private InfraManagerDataConfig infraManagerDataConfig;


  @Bean
  public ServletRegistrationBean jerseyServlet() {
    ServletRegistrationBean jerseyServletBean = new ServletRegistrationBean(new ServletContainer(), "/api/v1/*");
    jerseyServletBean.addInitParameter(ServletProperties.JAXRS_APPLICATION_CLASS, InfraManagerJerseyResourceConfig.class.getName());
    return jerseyServletBean;
  }

  @Bean
  public ServletRegistrationBean dataServlet() {
    ServletRegistrationBean dataServletBean = new ServletRegistrationBean(new DefaultServlet(), "/files/*");
    dataServletBean.addInitParameter("dirAllowed","true");
    dataServletBean.addInitParameter("pathInfoOnly","true");
    dataServletBean.addInitParameter("resourceBase", infraManagerDataConfig.getDataFolder());
    return dataServletBean;
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
    serverProperties.getSession().getCookie().setName(INFRA_MANAGER_SESSIONID);
    serverProperties.setDisplayName(INFRA_MANAGER_APPLICATION_NAME);
    jettyEmbeddedServletContainerFactory.setPort(port);
    return jettyEmbeddedServletContainerFactory;
  }
}
