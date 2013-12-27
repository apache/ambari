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
import java.net.BindException;
import java.util.Map;

import javax.crypto.BadPaddingException;

import org.apache.ambari.eventdb.webservice.WorkflowJsonService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.rest.AgentResource;
import org.apache.ambari.server.api.AmbariPersistFilter;
import org.apache.ambari.server.api.rest.BootStrapResource;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.AmbariMetaService;
import org.apache.ambari.server.api.services.KeyService;
import org.apache.ambari.server.api.services.PersistKeyValueImpl;
import org.apache.ambari.server.api.services.PersistKeyValueService;
import org.apache.ambari.server.bootstrap.BootStrapImpl;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.ClusterControllerImpl;
import org.apache.ambari.server.controller.internal.StackDefinedPropertyProvider;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.resources.ResourceManager;
import org.apache.ambari.server.resources.api.rest.GetResource;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.CertificateManager;
import org.apache.ambari.server.security.SecurityFilter;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.apache.ambari.server.security.authorization.AmbariLocalUserDetailsService;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.unsecured.rest.CertificateDownload;
import org.apache.ambari.server.security.unsecured.rest.CertificateSign;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.StageUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import com.sun.jersey.spi.container.servlet.ServletContainer;

@Singleton
public class AmbariServer {
  private static Logger LOG = LoggerFactory.getLogger(AmbariServer.class);

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
  @Inject
  MetainfoDAO metainfoDAO;

  public String getServerOsType() {
    return configs.getServerOsType();
  }

  private static AmbariManagementController clusterController = null;

  public static AmbariManagementController getController() {
    return clusterController;
  }

  public void run() throws Exception {
    // Initialize meta info before heartbeat monitor
    ambariMetaInfo.init();
    LOG.info("********* Meta Info initialized **********");

    performStaticInjection();
    addInMemoryUsers();
    server = new Server();
    serverForAgent = new Server();

    checkDBVersion();

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

      ServletContextHandler root = new ServletContextHandler(server, CONTEXT_PATH,
          ServletContextHandler.SECURITY | ServletContextHandler.SESSIONS);

      //Changing session cookie name to avoid conflicts
      root.getSessionHandler().getSessionManager().setSessionCookie("AMBARISESSIONID");

      GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
      springWebAppContext.setServletContext(root.getServletContext());
      springWebAppContext.setParent(springAppContext);
      /* Configure web app context */
      root.setResourceBase(configs.getWebAppDir());

      root.getServletContext().setAttribute(
          WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
          springWebAppContext);

      certMan.initRootCert();

      ServletContextHandler agentroot = new ServletContextHandler(serverForAgent,
          "/", ServletContextHandler.SESSIONS );

      ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);

      /* Configure default servlet for agent server */
      rootServlet = agentroot.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);

      //Spring Security Filter initialization
      DelegatingFilterProxy springSecurityFilter = new DelegatingFilterProxy();
      springSecurityFilter.setTargetBeanName("springSecurityFilterChain");

      //session-per-request strategy for api and agents
      root.addFilter(new FilterHolder(injector.getInstance(AmbariPersistFilter.class)), "/api/*", 1);
      agentroot.addFilter(new FilterHolder(injector.getInstance(AmbariPersistFilter.class)), "/agent/*", 1);

      agentroot.addFilter(SecurityFilter.class, "/*", 1);

      if (configs.getApiAuthentication()) {
        root.addFilter(new FilterHolder(springSecurityFilter), "/api/*", 1);
      }


      //Secured connector for 2-way auth
      SslSelectChannelConnector sslConnectorTwoWay = new
          SslSelectChannelConnector();
      sslConnectorTwoWay.setPort(configs.getTwoWayAuthPort());

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
      sslConnectorTwoWay.setNeedClientAuth(configs.getTwoWaySsl());

      //Secured connector for 1-way auth
      //SslSelectChannelConnector sslConnectorOneWay = new SslSelectChannelConnector();
      SslContextFactory contextFactory = new SslContextFactory(true);
      //sslConnectorOneWay.setPort(AGENT_ONE_WAY_AUTH);
      contextFactory.setKeyStorePath(keystore);
      // sslConnectorOneWay.setKeystore(keystore);
      contextFactory.setTrustStore(keystore);
      // sslConnectorOneWay.setTruststore(keystore);
      contextFactory.setKeyStorePassword(srvrCrtPass);
      // sslConnectorOneWay.setPassword(srvrCrtPass);

      contextFactory.setKeyManagerPassword(srvrCrtPass);

      // sslConnectorOneWay.setKeyPassword(srvrCrtPass);

      contextFactory.setTrustStorePassword(srvrCrtPass);
      //sslConnectorOneWay.setTrustPassword(srvrCrtPass);

      contextFactory.setKeyStoreType("PKCS12");
      //sslConnectorOneWay.setKeystoreType("PKCS12");
      contextFactory.setTrustStoreType("PKCS12");

      //sslConnectorOneWay.setTruststoreType("PKCS12");
      contextFactory.setNeedClientAuth(false);
      // sslConnectorOneWay.setWantClientAuth(false);
      // sslConnectorOneWay.setNeedClientAuth(false);
      SslSelectChannelConnector sslConnectorOneWay = new SslSelectChannelConnector(contextFactory);
      sslConnectorOneWay.setPort(configs.getOneWayAuthPort());
      sslConnectorOneWay.setAcceptors(2);
      sslConnectorTwoWay.setAcceptors(2);
      serverForAgent.setConnectors(new Connector[]{ sslConnectorOneWay, sslConnectorTwoWay});

      ServletHolder sh = new ServletHolder(ServletContainer.class);
      sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
          "com.sun.jersey.api.core.PackagesResourceConfig");
      sh.setInitParameter("com.sun.jersey.config.property.packages",
          "org.apache.ambari.server.api.rest;" +
              "org.apache.ambari.server.api.services;" +
              "org.apache.ambari.eventdb.webservice;" +
              "org.apache.ambari.server.api");
      sh.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
          "true");
      if (configs.csrfProtectionEnabled()) {
        sh.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters",
            "com.sun.jersey.api.container.filter.CsrfProtectionFilter");
      }
      root.addServlet(sh, "/api/v1/*");
      sh.setInitOrder(2);

      ServletHolder agent = new ServletHolder(ServletContainer.class);
      agent.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
          "com.sun.jersey.api.core.PackagesResourceConfig");
      agent.setInitParameter("com.sun.jersey.config.property.packages",
          "org.apache.ambari.server.agent.rest;" + "org.apache.ambari.server.api");
      agent.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
          "true");
      agentroot.addServlet(agent, "/agent/v1/*");
      agent.setInitOrder(3);

      ServletHolder cert = new ServletHolder(ServletContainer.class);
      cert.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
          "com.sun.jersey.api.core.PackagesResourceConfig");
      cert.setInitParameter("com.sun.jersey.config.property.packages",
          "org.apache.ambari.server.security.unsecured.rest;" + "org.apache.ambari.server.api");
      cert.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
          "true");
      agentroot.addServlet(cert, "/*");
      cert.setInitOrder(4);

      ServletHolder resources = new ServletHolder(ServletContainer.class);
      resources.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
          "com.sun.jersey.api.core.PackagesResourceConfig");
      resources.setInitParameter("com.sun.jersey.config.property.packages",
          "org.apache.ambari.server.resources.api.rest;" + "org.apache.ambari.server.api");
      resources.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
          "true");
      root.addServlet(resources, "/resources/*");
      resources.setInitOrder(6);

      //Set jetty thread pool
      serverForAgent.setThreadPool(new QueuedThreadPool(25));
      server.setThreadPool(new QueuedThreadPool(25));

      /* Configure the API server to use the NIO connectors */
      SelectChannelConnector apiConnector;

      if (configs.getApiSSLAuthentication()) {
        String httpsKeystore = configsMap.get(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY) +
          File.separator + configsMap.get(Configuration.CLIENT_API_SSL_KSTR_NAME_KEY);
        LOG.info("API SSL Authentication is turned on. Keystore - " + httpsKeystore);        
        
        String httpsCrtPass = configsMap.get(Configuration.CLIENT_API_SSL_CRT_PASS_KEY);

        SslSelectChannelConnector sapiConnector = new SslSelectChannelConnector();
        sapiConnector.setPort(configs.getClientSSLApiPort());
        sapiConnector.setKeystore(httpsKeystore);
        sapiConnector.setTruststore(httpsKeystore);
        sapiConnector.setPassword(httpsCrtPass);
        sapiConnector.setKeyPassword(httpsCrtPass);
        sapiConnector.setTrustPassword(httpsCrtPass);
        sapiConnector.setKeystoreType("PKCS12");
        sapiConnector.setTruststoreType("PKCS12");
        sapiConnector.setMaxIdleTime(configs.getConnectionMaxIdleTime());
        apiConnector = sapiConnector;
      } 
      else  {
        apiConnector = new SelectChannelConnector();
        apiConnector.setPort(configs.getClientApiPort());
        apiConnector.setMaxIdleTime(configs.getConnectionMaxIdleTime());
      }

      server.addConnector(apiConnector);

      server.setStopAtShutdown(true);
      serverForAgent.setStopAtShutdown(true);
      springAppContext.start();

      String osType = getServerOsType();
      if (osType == null || osType.isEmpty()) {
        throw new RuntimeException(Configuration.OS_VERSION_KEY + " is not "
            + " set in the ambari.properties file");
      }

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

      LOG.info("********* Initializing Scheduled Request Manager **********");
      ExecutionScheduleManager executionScheduleManager = injector
        .getInstance(ExecutionScheduleManager.class);


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

      executionScheduleManager.start();
      LOG.info("********* Started Scheduled Request Manager **********");

      server.join();
      LOG.info("Joined the Server");
    } catch (BadPaddingException bpe){
      LOG.error("Bad keystore or private key password. " +
        "HTTPS certificate re-importing may be required.");
      throw bpe;
    } catch(BindException bindException) {
      LOG.error("Could not bind to server port - instance may already be running. " +
          "Terminating this instance.", bindException);
      throw bindException;
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

  protected void checkDBVersion() throws AmbariException {
    LOG.info("Checking DB store version");
    String schemaVersion = metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY).getMetainfoValue();
    String serverVersion = ambariMetaInfo.getServerVersion();
    if (! schemaVersion.equals(serverVersion)) {
      String error = "Current database store version is not compatible with " +
              "current server version"
              + ", serverVersion=" + serverVersion
              + ", schemaVersion=" + schemaVersion;
      LOG.warn(error);
      throw new AmbariException(error);
    }
    LOG.info("DB store version is compatible");
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
    KeyService.init(injector.getInstance(PersistKeyValueImpl.class));
    AmbariMetaService.init(injector.getInstance(AmbariMetaInfo.class));
    BootStrapResource.init(injector.getInstance(BootStrapImpl.class));
    StageUtils.setGson(injector.getInstance(Gson.class));
    WorkflowJsonService.setDBProperties(
        injector.getInstance(Configuration.class));
    SecurityFilter.init(injector.getInstance(Configuration.class));
    StackDefinedPropertyProvider.init(injector);
    AbstractControllerResourceProvider.init(injector.getInstance(ResourceProviderFactory.class));
  }

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new ControllerModule());
    
    AmbariServer server = null;
    try {
      LOG.info("Getting the controller");
      injector.getInstance(GuiceJpaInitializer.class);
      server = injector.getInstance(AmbariServer.class);
      CertificateManager certMan = injector.getInstance(CertificateManager.class);
      certMan.initRootCert();
      ComponentSSLConfiguration.instance().init(server.configs);
      if (server != null) {
        server.run();
      }
    } catch (Throwable t) {
      LOG.error("Failed to run the Ambari Server", t);
      if (server != null) {
        server.stop();
      }
      System.exit(-1);
    }
  }
}
