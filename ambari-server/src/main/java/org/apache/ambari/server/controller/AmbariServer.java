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
import java.net.Authenticator;
import java.net.BindException;
import java.net.PasswordAuthentication;
import java.util.Map;

import javax.crypto.BadPaddingException;

import org.apache.ambari.eventdb.webservice.WorkflowJsonService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.rest.AgentResource;
import org.apache.ambari.server.api.AmbariErrorHandler;
import org.apache.ambari.server.api.AmbariPersistFilter;
import org.apache.ambari.server.api.MethodOverrideFilter;
import org.apache.ambari.server.api.rest.BootStrapResource;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.KeyService;
import org.apache.ambari.server.api.services.PersistKeyValueImpl;
import org.apache.ambari.server.api.services.PersistKeyValueService;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.bootstrap.BootStrapImpl;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.AlertDefinitionResourceProvider;
import org.apache.ambari.server.controller.internal.AmbariPrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.BlueprintResourceProvider;
import org.apache.ambari.server.controller.internal.ClusterPrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.controller.internal.PermissionResourceProvider;
import org.apache.ambari.server.controller.internal.PrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.StackAdvisorResourceProvider;
import org.apache.ambari.server.controller.internal.StackDefinedPropertyProvider;
import org.apache.ambari.server.controller.internal.StackDependencyResourceProvider;
import org.apache.ambari.server.controller.internal.UserPrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.ViewPermissionResourceProvider;
import org.apache.ambari.server.controller.nagios.NagiosPropertyProvider;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.resources.ResourceManager;
import org.apache.ambari.server.resources.api.rest.GetResource;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.apache.ambari.server.security.CertificateManager;
import org.apache.ambari.server.security.SecurityFilter;
import org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.apache.ambari.server.security.authorization.AmbariLocalUserDetailsService;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.authorization.internal.AmbariInternalAuthenticationProvider;
import org.apache.ambari.server.security.ldap.AmbariLdapDataPopulator;
import org.apache.ambari.server.security.unsecured.rest.CertificateDownload;
import org.apache.ambari.server.security.unsecured.rest.CertificateSign;
import org.apache.ambari.server.security.unsecured.rest.ConnectionInfo;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.ambari.server.view.ViewRegistry;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import com.sun.jersey.spi.container.servlet.ServletContainer;

@Singleton
public class AmbariServer {
  private static Logger LOG = LoggerFactory.getLogger(AmbariServer.class);

  private Server server = null;

  public volatile boolean running = true; // true while controller runs

  final String CONTEXT_PATH = "/";
  final String SPRING_CONTEXT_LOCATION =
      "classpath:/webapp/WEB-INF/spring-security.xml";
  final String DISABLED_ENTRIES_SPLITTER = "\\|";

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
  @Inject
  @Named("dbInitNeeded")
  boolean dbInitNeeded;
  /**
   * The singleton view registry.
   */
  @Inject
  ViewRegistry viewRegistry;

  /**
   * The handler list for deployed web apps.
   */
  @Inject
  AmbariHandlerList handlerList;

  /**
   * Session manager.
   */
  @Inject
  SessionManager sessionManager;

  /**
   * Session ID manager.
   */
  @Inject
  SessionIdManager sessionIdManager;

  @Inject
  DelegatingFilterProxy springSecurityFilter;

  public String getServerOsType() {
    return configs.getServerOsType();
  }

  private static AmbariManagementController clusterController = null;

  public static AmbariManagementController getController() {
    return clusterController;
  }

  @SuppressWarnings("deprecation")
  public void run() throws Exception {
    // Initialize meta info before heartbeat monitor
    ambariMetaInfo.init();
    LOG.info("********* Meta Info initialized **********");

    performStaticInjection();
    initDB();
    server = new Server();
    server.setSessionIdManager(sessionIdManager);
    Server serverForAgent = new Server();

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
      factory.registerSingleton("ambariLdapDataPopulator",
          injector.getInstance(AmbariLdapDataPopulator.class));
      factory.registerSingleton("ambariAuthorizationFilter",
          injector.getInstance(AmbariAuthorizationFilter.class));
      factory.registerSingleton("ambariInternalAuthenticationProvider",
          injector.getInstance(AmbariInternalAuthenticationProvider.class));

      //Spring Security xml config depends on this Bean

      String[] contextLocations = {SPRING_CONTEXT_LOCATION};
      ClassPathXmlApplicationContext springAppContext = new
          ClassPathXmlApplicationContext(contextLocations, parentSpringAppContext);
      //setting ambari web context

      ServletContextHandler root = new ServletContextHandler(
          ServletContextHandler.SECURITY | ServletContextHandler.SESSIONS);

      configureRootHandler(root);
      configureSessionManager(sessionManager);
      root.getSessionHandler().setSessionManager(sessionManager);

      GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
      springWebAppContext.setServletContext(root.getServletContext());
      springWebAppContext.setParent(springAppContext);

      root.getServletContext().setAttribute(
          WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
          springWebAppContext);
      handlerList.setSpringWebAppContext(springWebAppContext);

      certMan.initRootCert();

      // the agent communication (heartbeats, registration, etc) is stateless
      // and does not use sessions.
      ServletContextHandler agentroot = new ServletContextHandler(
          serverForAgent, "/", ServletContextHandler.NO_SESSIONS);

      ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitParameter("dirAllowed", "false");
      rootServlet.setInitOrder(1);

      /* Configure default servlet for agent server */
      rootServlet = agentroot.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);

      //session-per-request strategy for api and agents
      root.addFilter(new FilterHolder(injector.getInstance(AmbariPersistFilter.class)), "/api/*", 1);
      root.addFilter(new FilterHolder(injector.getInstance(AmbariPersistFilter.class)), "/proxy/*", 1);
      root.addFilter(new FilterHolder(new MethodOverrideFilter()), "/api/*", 1);
      root.addFilter(new FilterHolder(new MethodOverrideFilter()), "/proxy/*", 1);

      agentroot.addFilter(new FilterHolder(injector.getInstance(AmbariPersistFilter.class)), "/agent/*", 1);
      agentroot.addFilter(SecurityFilter.class, "/*", 1);

      if (configs.getApiAuthentication()) {
        root.addFilter(new FilterHolder(springSecurityFilter), "/api/*", 1);
        root.addFilter(new FilterHolder(springSecurityFilter), "/proxy/*", 1);
      }


      //Secured connector for 2-way auth
      SslContextFactory contextFactoryTwoWay = new SslContextFactory();
      disableInsecureProtocols(contextFactoryTwoWay);
      SslSelectChannelConnector sslConnectorTwoWay = new
          SslSelectChannelConnector(contextFactoryTwoWay);
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

      //SSL Context Factory
      SslContextFactory contextFactoryOneWay = new SslContextFactory(true);
      contextFactoryOneWay.setKeyStorePath(keystore);
      contextFactoryOneWay.setTrustStore(keystore);
      contextFactoryOneWay.setKeyStorePassword(srvrCrtPass);
      contextFactoryOneWay.setKeyManagerPassword(srvrCrtPass);
      contextFactoryOneWay.setTrustStorePassword(srvrCrtPass);
      contextFactoryOneWay.setKeyStoreType("PKCS12");
      contextFactoryOneWay.setTrustStoreType("PKCS12");
      contextFactoryOneWay.setNeedClientAuth(false);
      disableInsecureProtocols(contextFactoryOneWay);

      //Secured connector for 1-way auth
      SslSelectChannelConnector sslConnectorOneWay = new SslSelectChannelConnector(contextFactoryOneWay);
      sslConnectorOneWay.setPort(configs.getOneWayAuthPort());
      sslConnectorOneWay.setAcceptors(2);
      sslConnectorTwoWay.setAcceptors(2);
      serverForAgent.setConnectors(new Connector[]{sslConnectorOneWay, sslConnectorTwoWay});

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
      root.addServlet(sh, "/api/v1/*");
      sh.setInitOrder(2);

      SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

      viewRegistry.readViewArchives();

      handlerList.addHandler(root);

      server.setHandler(handlerList);

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

      ServletHolder proxy = new ServletHolder(ServletContainer.class);
      proxy.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
                             "com.sun.jersey.api.core.PackagesResourceConfig");
      proxy.setInitParameter("com.sun.jersey.config.property.packages",
                             "org.apache.ambari.server.proxy");
      proxy.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
      root.addServlet(proxy, "/proxy/*");
      proxy.setInitOrder(5);

      ServletHolder resources = new ServletHolder(ServletContainer.class);
      resources.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
          "com.sun.jersey.api.core.PackagesResourceConfig");
      resources.setInitParameter("com.sun.jersey.config.property.packages",
          "org.apache.ambari.server.resources.api.rest;");
      root.addServlet(resources, "/resources/*");
      resources.setInitOrder(6);

      if (configs.csrfProtectionEnabled()) {
        sh.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters",
                    "org.apache.ambari.server.api.AmbariCsrfProtectionFilter");
        proxy.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters",
                    "org.apache.ambari.server.api.AmbariCsrfProtectionFilter");
      }

      //Set jetty thread pool
      serverForAgent.setThreadPool(
          new QueuedThreadPool(configs.getAgentThreadPoolSize()));
      server.setThreadPool(
          new QueuedThreadPool(configs.getClientThreadPoolSize()));

      /* Configure the API server to use the NIO connectors */
      SelectChannelConnector apiConnector;

      if (configs.getApiSSLAuthentication()) {
        String httpsKeystore = configsMap.get(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY) +
          File.separator + configsMap.get(Configuration.CLIENT_API_SSL_KSTR_NAME_KEY);
        LOG.info("API SSL Authentication is turned on. Keystore - " + httpsKeystore);

        String httpsCrtPass = configsMap.get(Configuration.CLIENT_API_SSL_CRT_PASS_KEY);

        SslContextFactory contextFactoryApi = new SslContextFactory();
        disableInsecureProtocols(contextFactoryApi);
        SslSelectChannelConnector sapiConnector = new SslSelectChannelConnector(contextFactoryApi);
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
   * Disables insecure protocols and cipher suites (exact list is defined
   * at server properties)
   */
  private void disableInsecureProtocols(SslContextFactory factory) {
    if (! configs.getSrvrDisabledCiphers().isEmpty()) {
      String [] masks = configs.getSrvrDisabledCiphers().split(DISABLED_ENTRIES_SPLITTER);
      factory.setExcludeCipherSuites(masks);
    }
    if (! configs.getSrvrDisabledProtocols().isEmpty()) {
      String [] masks = configs.getSrvrDisabledProtocols().split(DISABLED_ENTRIES_SPLITTER);
      factory.setExcludeProtocols(masks);
    }
  }

  /**
   * Performs basic configuration of root handler with static values and values from
   * configuration file.
   *
   * @param root root handler
   */
  protected void configureRootHandler(ServletContextHandler root) {
    root.setContextPath(CONTEXT_PATH);
    root.setErrorHandler(injector.getInstance(AmbariErrorHandler.class));
    root.setMaxFormContentSize(-1);

    /* Configure web app context */
    root.setResourceBase(configs.getWebAppDir());
  }

  /**
   * Performs basic configuration of session manager with static values and values from
   * configuration file.
   *
   * @param sessionManager session manager
   */
  protected void configureSessionManager(SessionManager sessionManager) {
    // use AMBARISESSIONID instead of JSESSIONID to avoid conflicts with
    // other services (like HDFS) that run on the same context but a different
    // port
    sessionManager.setSessionCookie("AMBARISESSIONID");

    // each request that does not use AMBARISESSIONID will create a new
    // HashedSession in Jetty; these MUST be reaped after inactivity in order
    // to prevent a memory leak
    int sessionInactivityTimeout = configs.getHttpSessionInactiveTimeout();
    sessionManager.setMaxInactiveInterval(sessionInactivityTimeout);
  }

  /**
   * Creates default users if in-memory database is used
   */
  @Transactional
  protected void initDB() throws AmbariException {
    if (configs.getPersistenceType() == PersistenceType.IN_MEMORY || dbInitNeeded) {
      LOG.info("Database init needed - creating default data");
      Users users = injector.getInstance(Users.class);

      users.createUser("admin", "admin");
      users.createUser("user", "user");

      MetainfoEntity schemaVersion = new MetainfoEntity();
      schemaVersion.setMetainfoName(Configuration.SERVER_VERSION_KEY);
      schemaVersion.setMetainfoValue(ambariMetaInfo.getServerVersion());

      metainfoDAO.create(schemaVersion);
    }
  }

  protected void checkDBVersion() throws AmbariException {
    LOG.info("Checking DB store version");
    MetainfoEntity schemaVersionEntity = metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY);
    String schemaVersion = null;
    String serverVersion = null;

    if (schemaVersionEntity != null) {
      schemaVersion = schemaVersionEntity.getMetainfoValue();
      serverVersion = ambariMetaInfo.getServerVersion();
    }

    if (schemaVersionEntity==null || VersionUtils.compareVersions(schemaVersion, serverVersion, 3) != 0) {
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
    ConnectionInfo.init(injector.getInstance(Configuration.class));
    CertificateSign.init(injector.getInstance(CertificateManager.class));
    GetResource.init(injector.getInstance(ResourceManager.class));
    PersistKeyValueService.init(injector.getInstance(PersistKeyValueImpl.class));
    KeyService.init(injector.getInstance(PersistKeyValueImpl.class));
    BootStrapResource.init(injector.getInstance(BootStrapImpl.class));
    StackAdvisorResourceProvider.init(injector.getInstance(StackAdvisorHelper.class));
    StageUtils.setGson(injector.getInstance(Gson.class));
    WorkflowJsonService.setDBProperties(
        injector.getInstance(Configuration.class));
    SecurityFilter.init(injector.getInstance(Configuration.class));
    StackDefinedPropertyProvider.init(injector);
    NagiosPropertyProvider.init(injector);
    AbstractControllerResourceProvider.init(injector.getInstance(ResourceProviderFactory.class));
    BlueprintResourceProvider.init(injector.getInstance(BlueprintDAO.class),
        injector.getInstance(Gson.class), ambariMetaInfo);
    StackDependencyResourceProvider.init(ambariMetaInfo);
    ClusterResourceProvider.init(injector.getInstance(BlueprintDAO.class), ambariMetaInfo, injector.getInstance(ConfigHelper.class));
    AlertDefinitionResourceProvider.init(injector.getInstance(AlertDefinitionDAO.class));
    PermissionResourceProvider.init(injector.getInstance(PermissionDAO.class));
    ViewPermissionResourceProvider.init(injector.getInstance(PermissionDAO.class));
    PrivilegeResourceProvider.init(injector.getInstance(PrivilegeDAO.class), injector.getInstance(UserDAO.class),
        injector.getInstance(GroupDAO.class), injector.getInstance(PrincipalDAO.class),
        injector.getInstance(PermissionDAO.class), injector.getInstance(ResourceDAO.class));
    UserPrivilegeResourceProvider.init(injector.getInstance(UserDAO.class), injector.getInstance(ClusterDAO.class),
        injector.getInstance(GroupDAO.class), injector.getInstance(ViewInstanceDAO.class));
    ClusterPrivilegeResourceProvider.init(injector.getInstance(ClusterDAO.class));
    AmbariPrivilegeResourceProvider.init(injector.getInstance(ClusterDAO.class));
  }

  /**
   * Sets up proxy authentication.  This must be done before the server is
   * initialized since <code>AmbariMetaInfo</code> requires potential URL
   * lookups that may need the proxy.
   */
  static void setupProxyAuth() {
    final String proxyUser = System.getProperty("http.proxyUser");
    final String proxyPass = System.getProperty("http.proxyPassword");

    // to skip some hosts from proxy, pipe-separate names using, i.e.:
    // -Dhttp.nonProxyHosts=*.domain.com|host.internal.net

    if (null != proxyUser && null != proxyPass) {
      LOG.info("Proxy authentication enabled");

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
        }
      });
    } else {
      LOG.debug("Proxy authentication not specified");
    }
  }

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new ControllerModule());

    AmbariServer server = null;
    try {
      LOG.info("Getting the controller");

      setupProxyAuth();

      injector.getInstance(GuiceJpaInitializer.class);
      server = injector.getInstance(AmbariServer.class);
      CertificateManager certMan = injector.getInstance(CertificateManager.class);
      certMan.initRootCert();
      ViewRegistry.initInstance(server.viewRegistry);
      ComponentSSLConfiguration.instance().init(server.configs);
      server.run();
    } catch (Throwable t) {
      LOG.error("Failed to run the Ambari Server", t);
      if (server != null) {
        server.stop();
      }
      System.exit(-1);
    }
  }
}
