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
import java.util.Map;

import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.rest.AgentResource;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.AmbariMetaService;
import org.apache.ambari.server.api.services.PersistKeyValueImpl;
import org.apache.ambari.server.api.services.PersistKeyValueService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.resources.ResourceManager;
import org.apache.ambari.server.resources.api.rest.GetResource;
import org.apache.ambari.server.security.CertificateManager;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.apache.ambari.server.security.authorization.AmbariLocalUserDetailsService;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.unsecured.rest.CertificateDownload;
import org.apache.ambari.server.security.unsecured.rest.CertificateSign;
import org.apache.ambari.server.state.Clusters;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.sun.jersey.spi.container.servlet.ServletContainer;

@Singleton
public class AmbariServer {
  private static Logger LOG = LoggerFactory.getLogger(AmbariServer.class);
  public static final int CLIENT_ONE_WAY = 4080;
  public static final int CLIENT_TWO_WAY = 8443;
  public static final int CLIENT_API_PORT = 8080;
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
  @Inject
  AmbariMetaInfo ambariMetaInfo;

  private static AmbariManagementController clusterController = null;

  public static AmbariManagementController getController() {
    return clusterController;
  }

  private void setAmbariWebContext(Server server) {
    Context webContext = new Context(server, "/", 0);
    webContext.setHandler(new ResourceHandler());
    webContext.setResourceBase("web");
  }

  public void run() {
    performStaticInjection();
    addInMemoryUsers();
    server = new Server(CLIENT_API_PORT);
    serverForAgent = new Server();

    try {
      ClassPathXmlApplicationContext parentSpringAppContext =
          new ClassPathXmlApplicationContext();
      parentSpringAppContext.refresh();
      ConfigurableListableBeanFactory factory = parentSpringAppContext.
          getBeanFactory();
      factory.registerSingleton("guiceInjector",
          injector);
      factory.registerSingleton("passwordEncoder",
          injector.getInstance(PasswordEncoder.class));
      factory.registerSingleton("ambariLocalUserService",
          injector.getInstance(AmbariLocalUserDetailsService.class));
      factory.registerSingleton("ambariLdapAuthenticationProvider",
          injector.getInstance(AmbariLdapAuthenticationProvider.class));
      //Spring Security xml config depends on this Bean

      String[] contextLocations = {SPRING_CONTEXT_LOCATION};
      ClassPathXmlApplicationContext springAppContext = new
          ClassPathXmlApplicationContext(contextLocations, parentSpringAppContext);
       //setting ambari web context
      setAmbariWebContext(server);

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

      if (configs.getApiAuthentication()) {
        root.addFilter(new FilterHolder(springSecurityFilter), "/api/*", 1);
      }
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
              "org.apache.ambari.server.api.rest;" +
              "org.apache.ambari.server.api.services");
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

      LOG.info("********* Initializing Meta Info **********");
      ambariMetaInfo.init();

      //Start action scheduler
      LOG.info("********* Initializing Clusters **********");
      Clusters clusters = injector.getInstance(Clusters.class);
      StringBuilder clusterDump = new StringBuilder();
      clusters.debugDump(clusterDump);
      LOG.info("********* Current Clusters State *********");
      LOG.info(clusterDump.toString());

      LOG.info("********* Initializing ActionManager **********");
      ActionManager manager = injector.getInstance(ActionManager.class);
      LOG.info("********* Initializing Controller **********");
      AmbariManagementController controller = injector.getInstance(
          AmbariManagementController.class);

      clusterController = controller;

      // FIXME need to figure out correct order of starting things to
      // handle restart-recovery correctly

      /*
       * Start the server after controller state is recovered.
       */
      server.start();
      serverForAgent.start();
      LOG.info("********* Started Server **********");

      manager.start();
      LOG.info("********* Started ActionManager **********");

//TODO: Remove this code when APIs are ready for testing.
//      RequestInjectorForTest testInjector = new RequestInjectorForTest(controller, clusters);
//      Thread testInjectorThread = new Thread(testInjector);
//      testInjectorThread.start();

      server.join();
      LOG.info("Joined the Server");
    } catch (Exception e) {
      LOG.error("Error in the server", e);
    }
  }

  /**
   * Creates default users and roles if in-memory database is used
   */
  @Transactional
  protected void addInMemoryUsers() {
    if (configs.getPersistenceType() == PersistenceType.IN_MEMORY) {
      LOG.info("In-memory database is used - creating default users");
      Users users = injector.getInstance(Users.class);

      users.createDefaultRoles();
      users.createUser("admin", "admin");
      users.createUser("user", "user");
      try {
        users.promoteToAdmin(users.getLocalUser("admin"));
      } catch (AmbariException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void stop() throws Exception {
    try {
      server.stop();
    } catch (Exception e) {
      LOG.error("Error stopping the server", e);
    }
  }

  /**
   * Static injection replacement to wait Persistence Service start
   */
  public void performStaticInjection() {
    AgentResource.init(injector.getInstance(HeartBeatHandler.class));
    CertificateDownload.init(injector.getInstance(CertificateManager.class));
    CertificateSign.init(injector.getInstance(CertificateManager.class));
    GetResource.init(injector.getInstance(ResourceManager.class));
    PersistKeyValueService.init(injector.getInstance(PersistKeyValueImpl.class));
    AmbariMetaService.init(injector.getInstance(AmbariMetaInfo.class));
  }

  public static void main(String[] args) throws Exception {

    Injector injector = Guice.createInjector(new ControllerModule());

    try {
      LOG.info("Getting the controller");
      injector.getInstance(GuiceJpaInitializer.class);
      AmbariServer server = injector.getInstance(AmbariServer.class);
      CertificateManager certMan = injector.getInstance(CertificateManager.class);
      certMan.initRootCert();
      if (server != null) {
        server.run();
      }
    } catch (Throwable t) {
      LOG.error("Failed to run the Ambari Server", t);
    }
  }
}
