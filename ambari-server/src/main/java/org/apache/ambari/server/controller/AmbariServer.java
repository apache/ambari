/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller;


import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.BindException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.servlet.DispatcherType;

import org.apache.ambari.eventdb.webservice.WorkflowJsonService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StateRecoveryManager;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
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
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorBlueprintProcessor;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.bootstrap.BootStrapImpl;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.AmbariPrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.BaseClusterRequest;
import org.apache.ambari.server.controller.internal.BlueprintResourceProvider;
import org.apache.ambari.server.controller.internal.ClusterPrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.PermissionResourceProvider;
import org.apache.ambari.server.controller.internal.PrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.StackAdvisorResourceProvider;
import org.apache.ambari.server.controller.internal.StackDefinedPropertyProvider;
import org.apache.ambari.server.controller.internal.StackDependencyResourceProvider;
import org.apache.ambari.server.controller.internal.UserPrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.ViewPermissionResourceProvider;
import org.apache.ambari.server.controller.utilities.DatabaseChecker;
import org.apache.ambari.server.controller.utilities.KerberosChecker;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.PersistenceType;
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
import org.apache.ambari.server.security.AmbariServerSecurityHeaderFilter;
import org.apache.ambari.server.security.AmbariViewsSecurityHeaderFilter;
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
import org.apache.ambari.server.topology.AmbariContext;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.topology.TopologyRequestFactoryImpl;
import org.apache.ambari.server.utils.RetryHelper;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.velocity.app.Velocity;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.common.util.concurrent.ServiceManager;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import com.sun.jersey.spi.container.servlet.ServletContainer;

@Singleton
public class AmbariServer {
  private static Logger LOG = LoggerFactory.getLogger(AmbariServer.class);

  // Set velocity logger
  protected static final String VELOCITY_LOG_CATEGORY = "VelocityLogger";

  /**
   * Dispatcher types for webAppContext.addFilter.
   */
  public static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.of(DispatcherType.REQUEST);

  static {
    Velocity.setProperty("runtime.log.logsystem.log4j.logger", VELOCITY_LOG_CATEGORY);
  }

  private static final String CLASSPATH_CHECK_CLASS = "org/apache/ambari/server/controller/AmbariServer.class";
  private static final String CLASSPATH_SANITY_CHECK_FAILURE_MESSAGE = "%s class is found in multiple jar files. Possible reasons include multiple ambari server jar files in the ambari classpath.\n" +
      "Check for additional ambari server jar files and check that /usr/lib/ambari-server/ambari-server*.jar matches only one file.";

  static {
    Enumeration<URL> ambariServerClassUrls;
    try {
      ambariServerClassUrls = AmbariServer.class.getClassLoader().getResources(CLASSPATH_CHECK_CLASS);

      int ambariServerClassUrlsSize = 0;
      while (ambariServerClassUrls.hasMoreElements()) {
        ambariServerClassUrlsSize++;
        URL url = ambariServerClassUrls.nextElement();
        LOG.info(String.format("Found %s class in %s", CLASSPATH_CHECK_CLASS, url.getPath()));
      }
      if (ambariServerClassUrlsSize > 1) {
        throw new RuntimeException(String.format(CLASSPATH_SANITY_CHECK_FAILURE_MESSAGE, CLASSPATH_CHECK_CLASS));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

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
   * Guava service manager singleton (bound with {@link Scopes#SINGLETON}).
   */
  @Inject
  private ServiceManager serviceManager;

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

  /**
   * Alters system variables on base of Ambari configuration
   */
  static void setSystemProperties(Configuration configs) {
    // modify location of temporary dir to avoid using default /tmp dir
    System.setProperty("java.io.tmpdir", configs.getServerTempDir());
  }

  public static AmbariManagementController getController() {
    return clusterController;
  }

  @SuppressWarnings("deprecation")
  public void run() throws Exception {
    performStaticInjection();
    initDB();
    server = new Server();
    server.setSessionIdManager(sessionIdManager);
    Server serverForAgent = new Server();

    setSystemProperties(configs);

    DatabaseChecker.checkDBVersion();
    DatabaseChecker.checkDBConsistency();

    try {
      ClassPathXmlApplicationContext parentSpringAppContext =
          new ClassPathXmlApplicationContext();
      parentSpringAppContext.refresh();
      ConfigurableListableBeanFactory factory = parentSpringAppContext.
          getBeanFactory();

      factory.registerSingleton("guiceInjector", injector);
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

      // Spring Security xml config depends on this Bean
      String[] contextLocations = {SPRING_CONTEXT_LOCATION};
      ClassPathXmlApplicationContext springAppContext = new
          ClassPathXmlApplicationContext(contextLocations, parentSpringAppContext);

      ServletContextHandler root = new ServletContextHandler(
          ServletContextHandler.SECURITY | ServletContextHandler.SESSIONS);

      configureRootHandler(root);
      configureSessionManager(sessionManager);
      root.getSessionHandler().setSessionManager(sessionManager);

      // setting ambari web context
      GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
      springWebAppContext.setServletContext(root.getServletContext());
      springWebAppContext.setParent(springAppContext);

      root.getServletContext().setAttribute(
          WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
          springWebAppContext);

      certMan.initRootCert();

      // the agent communication (heartbeats, registration, etc) is stateless
      // and does not use sessions.
      ServletContextHandler agentroot = new ServletContextHandler(
          serverForAgent, "/", ServletContextHandler.NO_SESSIONS);

      if (configs.isAgentApiGzipped()) {
        configureHandlerCompression(agentroot);
      }

      ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitParameter("dirAllowed", "false");
      rootServlet.setInitOrder(1);

      /* Configure default servlet for agent server */
      rootServlet = agentroot.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);

      // Conditionally adds security-related headers to all HTTP responses.
      root.addFilter(new FilterHolder(injector.getInstance(AmbariServerSecurityHeaderFilter.class)), "/*", DISPATCHER_TYPES);

      // The security header filter - conditionally adds security-related headers to the HTTP response for Ambari Views
      // requests.
      root.addFilter(new FilterHolder(injector.getInstance(AmbariViewsSecurityHeaderFilter.class)), "/api/v1/views/*",
          DISPATCHER_TYPES);

      // session-per-request strategy for api
      root.addFilter(new FilterHolder(injector.getInstance(AmbariPersistFilter.class)), "/api/*", DISPATCHER_TYPES);
      root.addFilter(new FilterHolder(new MethodOverrideFilter()), "/api/*", DISPATCHER_TYPES);

      // register listener to capture request context
      root.addEventListener(new RequestContextListener());

      // session-per-request strategy for agents
      agentroot.addFilter(new FilterHolder(injector.getInstance(AmbariPersistFilter.class)), "/agent/*", DISPATCHER_TYPES);
      agentroot.addFilter(SecurityFilter.class, "/*", DISPATCHER_TYPES);

      if (configs.getApiAuthentication()) {
        root.addFilter(new FilterHolder(springSecurityFilter), "/api/*", DISPATCHER_TYPES);
        // root.addFilter(new FilterHolder(springSecurityFilter), "/proxy/*", DISPATCHER_TYPES);
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
      String truststore = configsMap.get(Configuration.SRVR_KSTR_DIR_KEY) +
          File.separator + configsMap.get(Configuration.TSTR_NAME_KEY);
      String srvrCrtPass = configsMap.get(Configuration.SRVR_CRT_PASS_KEY);
      sslConnectorTwoWay.setKeystore(keystore);
      sslConnectorTwoWay.setTruststore(truststore);
      sslConnectorTwoWay.setPassword(srvrCrtPass);
      sslConnectorTwoWay.setKeyPassword(srvrCrtPass);
      sslConnectorTwoWay.setTrustPassword(srvrCrtPass);
      sslConnectorTwoWay.setKeystoreType(configsMap.get(Configuration.KSTR_TYPE_KEY));
      sslConnectorTwoWay.setTruststoreType(configsMap.get(Configuration.TSTR_TYPE_KEY));
      sslConnectorTwoWay.setNeedClientAuth(configs.getTwoWaySsl());
      sslConnectorTwoWay.setRequestHeaderSize(configs.getHttpRequestHeaderSize());
      sslConnectorTwoWay.setResponseHeaderSize(configs.getHttpResponseHeaderSize());

      //SSL Context Factory
      SslContextFactory contextFactoryOneWay = new SslContextFactory(true);
      contextFactoryOneWay.setKeyStorePath(keystore);
      contextFactoryOneWay.setTrustStore(truststore);
      contextFactoryOneWay.setKeyStorePassword(srvrCrtPass);
      contextFactoryOneWay.setKeyManagerPassword(srvrCrtPass);
      contextFactoryOneWay.setTrustStorePassword(srvrCrtPass);
      contextFactoryOneWay.setKeyStoreType(configsMap.get(Configuration.KSTR_TYPE_KEY));
      contextFactoryOneWay.setTrustStoreType(configsMap.get(Configuration.TSTR_TYPE_KEY));
      contextFactoryOneWay.setNeedClientAuth(false);
      disableInsecureProtocols(contextFactoryOneWay);

      //Secured connector for 1-way auth
      SslSelectChannelConnector sslConnectorOneWay = new SslSelectChannelConnector(contextFactoryOneWay);
      sslConnectorOneWay.setPort(configs.getOneWayAuthPort());
      sslConnectorOneWay.setRequestHeaderSize(configs.getHttpRequestHeaderSize());
      sslConnectorOneWay.setResponseHeaderSize(configs.getHttpResponseHeaderSize());

      // because there are two connectors sharing the same pool, cut each's
      // acceptors in half
      int sslAcceptors = sslConnectorOneWay.getAcceptors();
      sslConnectorOneWay.setAcceptors(Math.max(2, sslAcceptors / 2));
      sslConnectorTwoWay.setAcceptors(Math.max(2, sslAcceptors / 2));

      // Agent Jetty thread pool
      configureJettyThreadPool(serverForAgent, sslConnectorOneWay.getAcceptors(),
          "qtp-ambari-agent", configs.getAgentThreadPoolSize());

      serverForAgent.addConnector(sslConnectorOneWay);
      serverForAgent.addConnector(sslConnectorTwoWay);

      ServletHolder sh = new ServletHolder(ServletContainer.class);
      sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
          "com.sun.jersey.api.core.PackagesResourceConfig");

      sh.setInitParameter("com.sun.jersey.config.property.packages",
        "org.apache.ambari.server.api.rest;" +
          "org.apache.ambari.server.api.services;" +
          "org.apache.ambari.eventdb.webservice;" +
          "org.apache.ambari.server.api");

      sh.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
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
      agent.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
      agentroot.addServlet(agent, "/agent/v1/*");
      agent.setInitOrder(3);

      AgentResource.statHeartBeatHandler();
      LOG.info("********** Started Heartbeat handler **********");

      ServletHolder cert = new ServletHolder(ServletContainer.class);
      cert.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
          "com.sun.jersey.api.core.PackagesResourceConfig");
      cert.setInitParameter("com.sun.jersey.config.property.packages",
          "org.apache.ambari.server.security.unsecured.rest;" + "org.apache.ambari.server.api");

      cert.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
      agentroot.addServlet(cert, "/*");
      cert.setInitOrder(4);

      ServletHolder resources = new ServletHolder(ServletContainer.class);
      resources.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
          "com.sun.jersey.api.core.PackagesResourceConfig");
      resources.setInitParameter("com.sun.jersey.config.property.packages",
          "org.apache.ambari.server.resources.api.rest;");
      root.addServlet(resources, "/resources/*");
      resources.setInitOrder(5);

      if (configs.csrfProtectionEnabled()) {
        sh.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters",
            "org.apache.ambari.server.api.AmbariCsrfProtectionFilter");
      }

      /* Configure the API server to use the NIO connectors */
      SelectChannelConnector apiConnector;

      if (configs.getApiSSLAuthentication()) {
        String httpsKeystore = configsMap.get(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY) +
            File.separator + configsMap.get(Configuration.CLIENT_API_SSL_KSTR_NAME_KEY);
        String httpsTruststore = configsMap.get(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY) +
            File.separator + configsMap.get(Configuration.CLIENT_API_SSL_TSTR_NAME_KEY);
        LOG.info("API SSL Authentication is turned on. Keystore - " + httpsKeystore);

        String httpsCrtPass = configsMap.get(Configuration.CLIENT_API_SSL_CRT_PASS_KEY);

        SslContextFactory contextFactoryApi = new SslContextFactory();
        disableInsecureProtocols(contextFactoryApi);
        SslSelectChannelConnector sapiConnector = new SslSelectChannelConnector(contextFactoryApi);
        sapiConnector.setPort(configs.getClientSSLApiPort());
        sapiConnector.setKeystore(httpsKeystore);
        sapiConnector.setTruststore(httpsTruststore);
        sapiConnector.setPassword(httpsCrtPass);
        sapiConnector.setKeyPassword(httpsCrtPass);
        sapiConnector.setTrustPassword(httpsCrtPass);
        sapiConnector.setKeystoreType(configsMap.get(Configuration.CLIENT_API_SSL_KSTR_TYPE_KEY));
        sapiConnector.setTruststoreType(configsMap.get(Configuration.CLIENT_API_SSL_KSTR_TYPE_KEY));
        sapiConnector.setMaxIdleTime(configs.getConnectionMaxIdleTime());
        apiConnector = sapiConnector;
      } else {
        apiConnector = new SelectChannelConnector();
        apiConnector.setPort(configs.getClientApiPort());
        apiConnector.setMaxIdleTime(configs.getConnectionMaxIdleTime());
      }

      apiConnector.setRequestHeaderSize(configs.getHttpRequestHeaderSize());
      apiConnector.setResponseHeaderSize(configs.getHttpResponseHeaderSize());

      // Client Jetty thread pool
      configureJettyThreadPool(server, apiConnector.getAcceptors(), "qtp-ambari-client", configs.getClientThreadPoolSize());
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

      LOG.info("********* Reconciling Alert Definitions **********");
      ambariMetaInfo.reconcileAlertDefinitions(clusters);

      LOG.info("********* Initializing ActionManager **********");
      ActionManager manager = injector.getInstance(ActionManager.class);

      LOG.info("********* Initializing Controller **********");
      AmbariManagementController controller = injector.getInstance(
          AmbariManagementController.class);

      LOG.info("********* Initializing Scheduled Request Manager **********");
      ExecutionScheduleManager executionScheduleManager = injector
          .getInstance(ExecutionScheduleManager.class);

      clusterController = controller;

      StateRecoveryManager recoveryManager = injector.getInstance(
          StateRecoveryManager.class);

      recoveryManager.doWork();

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

      serviceManager.startAsync();
      LOG.info("********* Started Services **********");

      server.join();
      LOG.info("Joined the Server");
    } catch (BadPaddingException bpe) {
      LOG.error("Bad keystore or private key password. " +
          "HTTPS certificate re-importing may be required.");
      throw bpe;
    } catch (BindException bindException) {
      LOG.error("Could not bind to server port - instance may already be running. " +
          "Terminating this instance.", bindException);
      throw bindException;
    }
  }

  /**
   * The Jetty thread pool consists of three basic types of threads:
   * <ul>
   * <li>Acceptors</li>
   * <li>Selectors</li>
   * <li>Threads which can actually do stuff</li>
   * <ul>
   * The {@link SelectChannelConnector} uses the
   * {@link Runtime#availableProcessors()} as a way to determine how many
   * acceptors and selectors to create. If the number of processors is too
   * great, then there will be no threads left to fullfil connection requests.
   * This method ensures that the pool size is configured correctly, taking into
   * account the number of available processors (sockets x core x
   * threads-per-core).
   * <p/>
   * If the configured pool size is determined to be too small, then this will
   * log a warning and increase the pool size to ensure that there are at least
   * 20 available threads for requests.
   *
   * @param server
   *          the Jetty server instance which will have the threadpool set on it
   *          (not {@code null}).
   * @param acceptorThreads
   *          the number of Acceptor threads configured for the connector.
   * @param threadPoolName
   *          the name of the thread pool being configured (not {@code null}).
   * @param configuredThreadPoolSize
   *          the size of the pool from {@link Configuration}.
   */
  protected void configureJettyThreadPool(Server server, int acceptorThreads,
      String threadPoolName, int configuredThreadPoolSize) {
    int minumumAvailableThreads = 20;

    // multiply by two since there is 1 selector for every acceptor
    int reservedJettyThreads = acceptorThreads * 2;

    // this is the calculation used by Jetty
    if (configuredThreadPoolSize < reservedJettyThreads + minumumAvailableThreads) {
      int newThreadPoolSize = reservedJettyThreads + minumumAvailableThreads;

      LOG.warn(
          "The configured Jetty {} thread pool value of {} is not sufficient on a host with {} processors. Increasing the value to {}.",
          threadPoolName, configuredThreadPoolSize, Runtime.getRuntime().availableProcessors(),
          newThreadPoolSize);

      configuredThreadPoolSize = newThreadPoolSize;
    }

    LOG.info(
        "Jetty is configuring {} with {} reserved acceptors/selectors and a total pool size of {} for {} processors.",
        threadPoolName, acceptorThreads * 2, configuredThreadPoolSize,
        Runtime.getRuntime().availableProcessors());

    QueuedThreadPool qtp = new QueuedThreadPool(configuredThreadPoolSize);
    qtp.setName(threadPoolName);
    server.setThreadPool(qtp);
  }

  /**
   * Disables insecure protocols and cipher suites (exact list is defined
   * at server properties)
   */
  private void disableInsecureProtocols(SslContextFactory factory) {
    if (!configs.getSrvrDisabledCiphers().isEmpty()) {
      String[] masks = configs.getSrvrDisabledCiphers().split(DISABLED_ENTRIES_SPLITTER);
      factory.setExcludeCipherSuites(masks);
    }
    if (!configs.getSrvrDisabledProtocols().isEmpty()) {
      String[] masks = configs.getSrvrDisabledProtocols().split(DISABLED_ENTRIES_SPLITTER);
      factory.setExcludeProtocols(masks);
    }
  }

  /**
   * Performs basic configuration of root handler with static values and values
   * from configuration file.
   *
   * @param root root handler
   */
  protected void configureRootHandler(ServletContextHandler root) {
    configureHandlerCompression(root);
    root.setContextPath(CONTEXT_PATH);
    root.setErrorHandler(injector.getInstance(AmbariErrorHandler.class));
    root.setMaxFormContentSize(-1);

    /* Configure web app context */
    root.setResourceBase(configs.getWebAppDir());
  }

  /**
   * Performs GZIP compression configuration of the context handler
   * with static values and values from configuration file
   *
   * @param context handler
   */
  protected void configureHandlerCompression(ServletContextHandler context) {
    if (configs.isApiGzipped()) {
      FilterHolder gzipFilter = context.addFilter(GzipFilter.class, "/*",
          EnumSet.of(DispatcherType.REQUEST));

      gzipFilter.setInitParameter("methods", "GET,POST,PUT,DELETE");
      gzipFilter.setInitParameter("mimeTypes",
          "text/html,text/plain,text/xml,text/css,application/x-javascript," +
              "application/xml,application/x-www-form-urlencoded," +
              "application/javascript,application/json");
      gzipFilter.setInitParameter("minGzipSize", configs.getApiGzipMinSize());
    }
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
    sessionManager.getSessionCookieConfig().setName("AMBARISESSIONID");

    sessionManager.getSessionCookieConfig().setHttpOnly(true);
    if (configs.getApiSSLAuthentication()) {
      sessionManager.getSessionCookieConfig().setSecure(true);
    }

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
      schemaVersion.setMetainfoValue(VersionUtils.getVersionSubstring(ambariMetaInfo.getServerVersion()));

      metainfoDAO.create(schemaVersion);
    }
  }

  public void stop() throws Exception {
    if (server == null) {
      throw new AmbariException("Error stopping the server");
    } else {
      try {
        server.stop();
      } catch (Exception e) {
        LOG.error("Error stopping the server", e);
      }
    }
  }

  /**
   * Deprecated. Instead, use {@link StaticallyInject}.
   * <p/>
   * Static injection replacement to wait Persistence Service start.
   *
   * @see StaticallyInject
   */
  @Deprecated
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
    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    WorkflowJsonService.setDBProperties(
        injector.getInstance(Configuration.class));
    SecurityFilter.init(injector.getInstance(Configuration.class));
    StackDefinedPropertyProvider.init(injector);
    AbstractControllerResourceProvider.init(injector.getInstance(ResourceProviderFactory.class));
    BlueprintResourceProvider.init(injector.getInstance(BlueprintFactory.class),
        injector.getInstance(BlueprintDAO.class), injector.getInstance(SecurityConfigurationFactory.class),
        injector.getInstance(Gson.class), ambariMetaInfo);
    StackDependencyResourceProvider.init(ambariMetaInfo);
    ClusterResourceProvider.init(injector.getInstance(TopologyManager.class),
        injector.getInstance(TopologyRequestFactoryImpl.class), injector.getInstance(SecurityConfigurationFactory
            .class), injector.getInstance(Gson.class));
    HostResourceProvider.setTopologyManager(injector.getInstance(TopologyManager.class));
    BlueprintFactory.init(injector.getInstance(BlueprintDAO.class));
    BaseClusterRequest.init(injector.getInstance(BlueprintFactory.class));
    AmbariContext.init(injector.getInstance(HostRoleCommandFactory.class));

    PermissionResourceProvider.init(injector.getInstance(PermissionDAO.class));
    ViewPermissionResourceProvider.init(injector.getInstance(PermissionDAO.class));
    PrivilegeResourceProvider.init(injector.getInstance(PrivilegeDAO.class), injector.getInstance(UserDAO.class),
        injector.getInstance(GroupDAO.class), injector.getInstance(PrincipalDAO.class),
        injector.getInstance(PermissionDAO.class), injector.getInstance(ResourceDAO.class));
    UserPrivilegeResourceProvider.init(injector.getInstance(UserDAO.class), injector.getInstance(ClusterDAO.class),
        injector.getInstance(GroupDAO.class), injector.getInstance(ViewInstanceDAO.class));
    ClusterPrivilegeResourceProvider.init(injector.getInstance(ClusterDAO.class));
    AmbariPrivilegeResourceProvider.init(injector.getInstance(ClusterDAO.class));
    ActionManager.setTopologyManager(injector.getInstance(TopologyManager.class));
    StackAdvisorBlueprintProcessor.init(injector.getInstance(StackAdvisorHelper.class));

    RetryHelper.init(configs.getOperationsRetryAttempts());
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
      DatabaseChecker.checkDBVersion();
      server = injector.getInstance(AmbariServer.class);
      CertificateManager certMan = injector.getInstance(CertificateManager.class);
      certMan.initRootCert();
      KerberosChecker.checkJaasConfiguration();
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
