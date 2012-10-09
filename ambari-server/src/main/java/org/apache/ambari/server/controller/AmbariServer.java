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

package org.apache.ambari.server.controller;


import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.security.CertificateManager;
import org.apache.ambari.server.state.Clusters;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.sun.jersey.spi.container.servlet.ServletContainer;

@Singleton
public class AmbariServer {
  public static final String PERSISTENCE_PROVIDER = "ambari-postgres";
  private static Logger LOG = LoggerFactory.getLogger(AmbariServer.class);
  public static int CLIENT_ONE_WAY = 4080;
  public static int CLIENT_TWO_WAY = 8443;
  public static int CLIENT_API_PORT = 8080;
  private Server server = null;
  private Server serverForAgent = null;

  public volatile boolean running = true; // true while controller runs

  final String CONTEXT_PATH = "/";
  final String SPRING_CONTEXT_LOCATION =
      "classpath:/webapp/WEB-INF/spring-security.xml";

  @Inject
  Configuration configs;
  @Inject
  CertificateManager certMan;
  @Inject
  Injector injector;

  public void run() {
    server = new Server(CLIENT_API_PORT);
    serverForAgent = new Server();

    try {
      ClassPathXmlApplicationContext parentSpringAppContext =
          new ClassPathXmlApplicationContext();
      parentSpringAppContext.refresh();
      ConfigurableListableBeanFactory factory = parentSpringAppContext.
          getBeanFactory();
      factory.registerSingleton("guiceInjector", injector);
      //Spring Security xml config depends on this Bean

      String[] contextLocations = {SPRING_CONTEXT_LOCATION};
      ClassPathXmlApplicationContext springAppContext = new
          ClassPathXmlApplicationContext(contextLocations, parentSpringAppContext);

      Context root = new Context(server, CONTEXT_PATH, Context.ALL);

      GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
      springWebAppContext.setServletContext(root.getServletContext());
      springWebAppContext.setParent(springAppContext);

      root.getServletContext().setAttribute(
          WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
          springWebAppContext);

      certMan.initRootCert();

      Context agentroot = new Context(serverForAgent, "/", Context.SESSIONS);

      ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);

      /* Configure default servlet for agent server */
      rootServlet = agentroot.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);

      //Spring Security Filter initialization
      DelegatingFilterProxy springSecurityFilter = new DelegatingFilterProxy();
      springSecurityFilter.setTargetBeanName("springSecurityFilterChain");
      root.addFilter(new FilterHolder(springSecurityFilter), "/*", 1);

      //Secured connector for 2-way auth
      SslSocketConnector sslConnectorTwoWay = new SslSocketConnector();
      sslConnectorTwoWay.setPort(CLIENT_TWO_WAY);

      Map<String, String> configsMap = configs.getConfigsMap();
      String keystore = configsMap.get(Configuration.SRVR_KSTR_DIR_KEY) +
          File.separator + configsMap.get(Configuration.KSTR_NAME_KEY);
      String srvrCrtPass = configsMap.get(Configuration.SRVR_CRT_PASS_KEY);

      sslConnectorTwoWay.setKeystore(keystore);
      sslConnectorTwoWay.setTruststore(keystore);
      sslConnectorTwoWay.setPassword(srvrCrtPass);
      sslConnectorTwoWay.setKeyPassword(srvrCrtPass);
      sslConnectorTwoWay.setTrustPassword(srvrCrtPass);
      sslConnectorTwoWay.setKeystoreType("PKCS12");
      sslConnectorTwoWay.setTruststoreType("PKCS12");
      sslConnectorTwoWay.setNeedClientAuth(true);

      //Secured connector for 1-way auth
      SslSocketConnector sslConnectorOneWay = new SslSocketConnector();
      sslConnectorOneWay.setPort(CLIENT_ONE_WAY);

      sslConnectorOneWay.setKeystore(keystore);
      sslConnectorOneWay.setTruststore(keystore);
      sslConnectorOneWay.setPassword(srvrCrtPass);
      sslConnectorOneWay.setKeyPassword(srvrCrtPass);
      sslConnectorOneWay.setTrustPassword(srvrCrtPass);
      sslConnectorOneWay.setKeystoreType("PKCS12");
      sslConnectorOneWay.setTruststoreType("PKCS12");
      sslConnectorOneWay.setNeedClientAuth(false);



      serverForAgent.addConnector(sslConnectorOneWay);
      serverForAgent.addConnector(sslConnectorTwoWay);

      ServletHolder sh = new ServletHolder(ServletContainer.class);
      sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
              "com.sun.jersey.api.core.PackagesResourceConfig");
      sh.setInitParameter("com.sun.jersey.config.property.packages",
              "org.apache.ambari.server.api.rest");
      root.addServlet(sh, "/api/*");
      sh.setInitOrder(2);

      ServletHolder agent = new ServletHolder(ServletContainer.class);
      agent.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
              "com.sun.jersey.api.core.PackagesResourceConfig");
      agent.setInitParameter("com.sun.jersey.config.property.packages",
              "org.apache.ambari.server.agent.rest");
      agent.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
          "true");
      agentroot.addServlet(agent, "/agent/*");
      agent.setInitOrder(3);

      ServletHolder cert = new ServletHolder(ServletContainer.class);
      cert.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
              "com.sun.jersey.api.core.PackagesResourceConfig");
      cert.setInitParameter("com.sun.jersey.config.property.packages",
              "org.apache.ambari.server.security.unsecured.rest");
      agentroot.addServlet(cert, "/*");
      cert.setInitOrder(4);

      ServletHolder resources = new ServletHolder(ServletContainer.class);
      resources.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
              "com.sun.jersey.api.core.PackagesResourceConfig");
      resources.setInitParameter("com.sun.jersey.config.property.packages",
        "org.apache.ambari.server.resources.api.rest");
      root.addServlet(resources, "/resources/*");
      resources.setInitOrder(6);
      server.setStopAtShutdown(true);
      serverForAgent.setStopAtShutdown(true);
      springAppContext.start();
      /*
       * Start the server after controller state is recovered.
       */
      server.start();
      serverForAgent.start();

      //Start action scheduler
      LOG.info("********* Initializing Clusters **********");
      Clusters clusters = injector.getInstance(Clusters.class);
      LOG.info("********* Started Server **********");
      ActionManager manager = injector.getInstance(ActionManager.class);
      manager.start();
      LOG.info("********* Initializing Controller **********");
      AmbariManagementController controller = injector.getInstance(
          AmbariManagementController.class);

      server.join();
      LOG.info("Joined the Server");
    } catch (Exception e) {
      LOG.error("Error in the server", e);
    }
  }

  public void stop() throws Exception {
    try {
      server.stop();
    } catch (Exception e) {
      LOG.error("Error stopping the server", e);
    }
  }

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new ControllerModule(),
        new JpaPersistModule(PERSISTENCE_PROVIDER));

    try {
      LOG.info("Getting the controller");
      AmbariServer server = injector.getInstance(AmbariServer.class);
      CertificateManager certMan = injector.getInstance(CertificateManager.class);
      injector.getInstance(GuiceJpaInitializer.class);
      certMan.initRootCert();
      if (server != null) {
        server.run();
      }
    } catch (Throwable t) {
      LOG.error("Failed to run the Ambari Server", t);
    }
  }
}
