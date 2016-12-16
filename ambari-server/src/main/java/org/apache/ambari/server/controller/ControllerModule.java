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

package org.apache.ambari.server.controller;

import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_JDBC_DDL_FILE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_ONLY;
import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_OR_EXTEND;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_BOTH_GENERATION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION_MODE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DROP_AND_CREATE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DROP_JDBC_DDL_FILE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.NON_JTA_DATASOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.THROW_EXCEPTIONS;

import java.beans.PropertyVetoException;
import java.lang.annotation.Annotation;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.AmbariService;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapperFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.actionmanager.StageFactoryImpl;
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.checks.DatabaseConsistencyCheckHelper;
import org.apache.ambari.server.checks.UpgradeCheckRegistry;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.ConnectionPoolType;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
import org.apache.ambari.server.controller.internal.CredentialResourceProvider;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostKerberosIdentityResourceProvider;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.KerberosDescriptorResourceProvider;
import org.apache.ambari.server.controller.internal.MemberResourceProvider;
import org.apache.ambari.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactory;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactoryImpl;
import org.apache.ambari.server.controller.metrics.MetricPropertyProviderFactory;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheEntryFactory;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.KerberosChecker;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.hooks.AmbariEventFactory;
import org.apache.ambari.server.hooks.HookContext;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.PostUserCreationHookContext;
import org.apache.ambari.server.hooks.users.UserCreatedEvent;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.metrics.system.MetricsService;
import org.apache.ambari.server.metrics.system.impl.MetricsServiceImpl;
import org.apache.ambari.server.notifications.DispatchFactory;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.dispatchers.AmbariSNMPDispatcher;
import org.apache.ambari.server.notifications.dispatchers.SNMPDispatcher;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.scheduler.ExecutionSchedulerImpl;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.SecurityHelperImpl;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationHandlerFactory;
import org.apache.ambari.server.serveraction.users.CollectionPersisterService;
import org.apache.ambari.server.serveraction.users.CollectionPersisterServiceFactory;
import org.apache.ambari.server.serveraction.users.CsvFilePersisterService;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceComponentImpl;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroupImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptorFactory;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionImpl;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.view.ViewInstanceHandlerList;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.util.ClassUtils;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.common.util.concurrent.ServiceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.jpa.AmbariJpaPersistModule;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Used for injection purposes.
 */
public class ControllerModule extends AbstractModule {
  private static Logger LOG = LoggerFactory.getLogger(ControllerModule.class);
  private static final String AMBARI_PACKAGE = "org.apache.ambari.server";

  private final Configuration configuration;
  private final OsFamily os_family;
  private final HostsMap hostsMap;
  private boolean dbInitNeeded;
  private final Gson prettyGson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();


  // ----- Constructors ------------------------------------------------------

  public ControllerModule() throws Exception {
    configuration = new Configuration();
    hostsMap = new HostsMap(configuration);
    os_family = new OsFamily(configuration);
  }

  public ControllerModule(Properties properties) throws Exception {
    configuration = new Configuration(properties);
    hostsMap = new HostsMap(configuration);
    os_family = new OsFamily(configuration);
  }


  // ----- ControllerModule --------------------------------------------------

  /**
   * Get the common persistence related configuration properties.
   *
   * @return the configuration properties
   */
  public static Properties getPersistenceProperties(Configuration configuration) {
    Properties properties = new Properties();

    // log what database type has been calculated
    DatabaseType databaseType = configuration.getDatabaseType();
    LOG.info("Detected {} as the database type from the JDBC URL", databaseType);

    // custom jdbc driver properties
    Properties customDatabaseDriverProperties = configuration.getDatabaseCustomProperties();
    properties.putAll(customDatabaseDriverProperties);

    // custom persistence properties
    Properties customPersistenceProperties = configuration.getPersistenceCustomProperties();
    properties.putAll(customPersistenceProperties);

    switch (configuration.getPersistenceType()) {
      case IN_MEMORY:
        properties.setProperty(JDBC_URL, Configuration.JDBC_IN_MEMORY_URL);
        properties.setProperty(JDBC_DRIVER, Configuration.JDBC_IN_MEMORY_DRIVER);
        properties.setProperty(DDL_GENERATION, DROP_AND_CREATE);
        properties.setProperty(THROW_EXCEPTIONS, "true");
        break;
      case REMOTE:
        properties.setProperty(JDBC_URL, configuration.getDatabaseUrl());
        properties.setProperty(JDBC_DRIVER, configuration.getDatabaseDriver());
        break;
      case LOCAL:
        properties.setProperty(JDBC_URL, configuration.getLocalDatabaseUrl());
        properties.setProperty(JDBC_DRIVER, Configuration.SERVER_JDBC_DRIVER.getDefaultValue());
        break;
    }

    // determine the type of pool to use
    boolean isConnectionPoolingExternal = false;
    ConnectionPoolType connectionPoolType = configuration.getConnectionPoolType();
    if (connectionPoolType == ConnectionPoolType.C3P0) {
      isConnectionPoolingExternal = true;
    }

    // force the use of c3p0 with MySQL
    if (databaseType == DatabaseType.MYSQL) {
      isConnectionPoolingExternal = true;
    }

    // use c3p0
    if (isConnectionPoolingExternal) {
      LOG.info("Using c3p0 {} as the EclipsLink DataSource",
          ComboPooledDataSource.class.getSimpleName());

      // Oracle requires a different validity query
      String testQuery = "SELECT 1";
      if (databaseType == DatabaseType.ORACLE) {
        testQuery = "SELECT 1 FROM DUAL";
      }

      ComboPooledDataSource dataSource = new ComboPooledDataSource();

      // attempt to load the driver; if this fails, warn and move on
      try {
        dataSource.setDriverClass(configuration.getDatabaseDriver());
      } catch (PropertyVetoException pve) {
        LOG.warn("Unable to initialize c3p0", pve);
        return properties;
      }

      // basic configuration stuff
      dataSource.setJdbcUrl(configuration.getDatabaseUrl());
      dataSource.setUser(configuration.getDatabaseUser());
      dataSource.setPassword(configuration.getDatabasePassword());

      // pooling
      dataSource.setMinPoolSize(configuration.getConnectionPoolMinimumSize());
      dataSource.setInitialPoolSize(configuration.getConnectionPoolMinimumSize());
      dataSource.setMaxPoolSize(configuration.getConnectionPoolMaximumSize());
      dataSource.setAcquireIncrement(configuration.getConnectionPoolAcquisitionSize());
      dataSource.setAcquireRetryAttempts(configuration.getConnectionPoolAcquisitionRetryAttempts());
      dataSource.setAcquireRetryDelay(configuration.getConnectionPoolAcquisitionRetryDelay());

      // validity
      dataSource.setMaxConnectionAge(configuration.getConnectionPoolMaximumAge());
      dataSource.setMaxIdleTime(configuration.getConnectionPoolMaximumIdle());
      dataSource.setMaxIdleTimeExcessConnections(configuration.getConnectionPoolMaximumExcessIdle());
      dataSource.setPreferredTestQuery(testQuery);
      dataSource.setIdleConnectionTestPeriod(configuration.getConnectionPoolIdleTestInternval());

      properties.put(NON_JTA_DATASOURCE, dataSource);
    }

    return properties;
  }


  // ----- AbstractModule ----------------------------------------------------

  @Override
  protected void configure() {
    installFactories();

    final SessionIdManager sessionIdManager = new HashSessionIdManager();
    final SessionManager sessionManager = new HashSessionManager();
    sessionManager.getSessionCookieConfig().setPath("/");
    sessionManager.setSessionIdManager(sessionIdManager);
    bind(SessionManager.class).toInstance(sessionManager);
    bind(SessionIdManager.class).toInstance(sessionIdManager);

    bind(KerberosOperationHandlerFactory.class);
    bind(KerberosDescriptorFactory.class);
    bind(KerberosServiceDescriptorFactory.class);
    bind(KerberosHelper.class).to(KerberosHelperImpl.class);

    bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);

    bind(Configuration.class).toInstance(configuration);
    bind(OsFamily.class).toInstance(os_family);
    bind(HostsMap.class).toInstance(hostsMap);
    bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
    bind(DelegatingFilterProxy.class).toInstance(new DelegatingFilterProxy() {
      {
        setTargetBeanName("springSecurityFilterChain");
      }
    });

    bind(Gson.class).annotatedWith(Names.named("prettyGson")).toInstance(prettyGson);

    install(buildJpaPersistModule());

    bind(Gson.class).in(Scopes.SINGLETON);
    bind(SecureRandom.class).in(Scopes.SINGLETON);

    bind(Clusters.class).to(ClustersImpl.class);
    bind(AmbariCustomCommandExecutionHelper.class);
    bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
    bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(
        configuration.getExecutionSchedulerWait());

    // This time is added to summary timeout time of all tasks in stage
    // So it's an "additional time", given to stage to finish execution before
    // it is considered as timed out
    bindConstant().annotatedWith(Names.named("actionTimeout")).to(600000L);

    bindConstant().annotatedWith(Names.named("dbInitNeeded")).to(dbInitNeeded);
    bindConstant().annotatedWith(Names.named("statusCheckInterval")).to(5000L);

    //ExecutionCommands cache size

    bindConstant().annotatedWith(Names.named("executionCommandCacheSize")).
        to(configuration.getExecutionCommandsCacheSize());


    // Host role commands status summary max cache enable/disable
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_ENABLED)).
        to(configuration.getHostRoleCommandStatusSummaryCacheEnabled());

    // Host role commands status summary max cache size
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_SIZE)).
        to(configuration.getHostRoleCommandStatusSummaryCacheSize());
    // Host role command status summary cache expiry duration in minutes
    bindConstant().annotatedWith(Names.named(HostRoleCommandDAO.HRC_STATUS_SUMMARY_CACHE_EXPIRY_DURATION_MINUTES)).
        to(configuration.getHostRoleCommandStatusSummaryCacheExpiryDuration());

    bind(AmbariManagementController.class).to(
        AmbariManagementControllerImpl.class);
    bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
    bind(ExecutionScheduler.class).to(ExecutionSchedulerImpl.class);
    bind(DBAccessor.class).to(DBAccessorImpl.class);
    bind(ViewInstanceHandlerList.class).to(AmbariHandlerList.class);
    bind(TimelineMetricCacheProvider.class);
    bind(TimelineMetricCacheEntryFactory.class);
    bind(SecurityConfigurationFactory.class).in(Scopes.SINGLETON);

    bind(PersistedState.class).to(PersistedStateImpl.class);

    // factory to create LoggingRequestHelper instances for LogSearch integration
    bind(LoggingRequestHelperFactory.class).to(LoggingRequestHelperFactoryImpl.class);

    bind(MetricsService.class).to(MetricsServiceImpl.class).in(Scopes.SINGLETON);

    requestStaticInjection(DatabaseConsistencyCheckHelper.class);
    requestStaticInjection(KerberosChecker.class);
    requestStaticInjection(AuthorizationHelper.class);

    bindByAnnotation(null);
    bindNotificationDispatchers(null);
    registerUpgradeChecks(null);
    bind(HookService.class).to(UserHookService.class);
  }

  // ----- helper methods ----------------------------------------------------

  private PersistModule buildJpaPersistModule() {
    PersistenceType persistenceType = configuration.getPersistenceType();
    AmbariJpaPersistModule jpaPersistModule = new AmbariJpaPersistModule(Configuration.JDBC_UNIT_NAME);

    Properties persistenceProperties = ControllerModule.getPersistenceProperties(configuration);

    if (!persistenceType.equals(PersistenceType.IN_MEMORY)) {
      persistenceProperties.setProperty(JDBC_USER, configuration.getDatabaseUser());
      persistenceProperties.setProperty(JDBC_PASSWORD, configuration.getDatabasePassword());

      switch (configuration.getJPATableGenerationStrategy()) {
        case CREATE:
          persistenceProperties.setProperty(DDL_GENERATION, CREATE_ONLY);
          dbInitNeeded = true;
          break;
        case DROP_AND_CREATE:
          persistenceProperties.setProperty(DDL_GENERATION, DROP_AND_CREATE);
          dbInitNeeded = true;
          break;
        case CREATE_OR_EXTEND:
          persistenceProperties.setProperty(DDL_GENERATION, CREATE_OR_EXTEND);
          break;
        default:
          break;
      }

      persistenceProperties.setProperty(DDL_GENERATION_MODE, DDL_BOTH_GENERATION);
      persistenceProperties.setProperty(CREATE_JDBC_DDL_FILE, "DDL-create.jdbc");
      persistenceProperties.setProperty(DROP_JDBC_DDL_FILE, "DDL-drop.jdbc");
    }

    jpaPersistModule.properties(persistenceProperties);
    return jpaPersistModule;
  }

  /**
   * Bind classes to their Factories, which can be built on-the-fly.
   * Often, will also have to edit AgentResourceTest.java
   */
  private void installFactories() {
    install(new FactoryModuleBuilder().implement(
        Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
    install(new FactoryModuleBuilder().implement(
        Host.class, HostImpl.class).build(HostFactory.class));
    install(new FactoryModuleBuilder().implement(
        Service.class, ServiceImpl.class).build(ServiceFactory.class));

    install(new FactoryModuleBuilder()
        .implement(ResourceProvider.class, Names.named("host"), HostResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("hostComponent"), HostComponentResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("service"), ServiceResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("component"), ComponentResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("member"), MemberResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("repositoryVersion"), RepositoryVersionResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("hostKerberosIdentity"), HostKerberosIdentityResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("credential"), CredentialResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("kerberosDescriptor"), KerberosDescriptorResourceProvider.class)
        .implement(ResourceProvider.class, Names.named("upgrade"), UpgradeResourceProvider.class)
        .build(ResourceProviderFactory.class));

    install(new FactoryModuleBuilder().implement(
        ServiceComponent.class, ServiceComponentImpl.class).build(
        ServiceComponentFactory.class));
    install(new FactoryModuleBuilder().implement(
        ServiceComponentHost.class, ServiceComponentHostImpl.class).build(
        ServiceComponentHostFactory.class));
    install(new FactoryModuleBuilder().implement(
        Config.class, ConfigImpl.class).build(ConfigFactory.class));
    install(new FactoryModuleBuilder().implement(
        ConfigGroup.class, ConfigGroupImpl.class).build(ConfigGroupFactory.class));
    install(new FactoryModuleBuilder().implement(RequestExecution.class,
        RequestExecutionImpl.class).build(RequestExecutionFactory.class));

    bind(StageFactory.class).to(StageFactoryImpl.class);
    bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);

    install(new FactoryModuleBuilder().build(RoleGraphFactory.class));

    install(new FactoryModuleBuilder().build(RequestFactory.class));
    install(new FactoryModuleBuilder().build(StackManagerFactory.class));
    install(new FactoryModuleBuilder().build(ExecutionCommandWrapperFactory.class));
    install(new FactoryModuleBuilder().build(MetricPropertyProviderFactory.class));

    bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
    bind(SecurityHelper.class).toInstance(SecurityHelperImpl.getInstance());
    bind(BlueprintFactory.class);

    install(new FactoryModuleBuilder().implement(AmbariEvent.class, Names.named("userCreated"), UserCreatedEvent.class).build(AmbariEventFactory.class));
    install(new FactoryModuleBuilder().implement(HookContext.class, PostUserCreationHookContext.class).build(HookContextFactory.class));
    install(new FactoryModuleBuilder().implement(CollectionPersisterService.class, CsvFilePersisterService.class).build(CollectionPersisterServiceFactory.class));

  }

  /**
   * Initializes specially-marked interfaces that require injection.
   * <p/>
   * An example of where this is needed is with a singleton that is headless; in
   * other words, it doesn't have any injections but still needs to be part of
   * the Guice framework.
   * <p/>
   * A second example of where this is needed is when classes require static
   * members that are available via injection.
   * <p/>
   * If {@code beanDefinitions} is empty or null this will scan
   * {@code org.apache.ambari.server} (currently) for any {@link EagerSingleton}
   * or {@link StaticallyInject} or {@link AmbariService} instances.
   *
   * @param beanDefinitions the set of bean definitions. If it is empty or
   *                        {@code null} scan will occur.
   * @return the set of bean definitions that was found during scan if
   * {@code beanDefinitions} was null or empty. Else original
   * {@code beanDefinitions} will be returned.
   */
  // Method is protected and returns a set of bean definitions for testing convenience.
  @SuppressWarnings("unchecked")
  protected Set<BeanDefinition> bindByAnnotation(Set<BeanDefinition> beanDefinitions) {
    List<Class<? extends Annotation>> classes = Arrays.asList(
        EagerSingleton.class, StaticallyInject.class, AmbariService.class);

    if (null == beanDefinitions || beanDefinitions.size() == 0) {
      ClassPathScanningCandidateComponentProvider scanner =
          new ClassPathScanningCandidateComponentProvider(false);

      // match only singletons that are eager listeners
      for (Class<? extends Annotation> cls : classes) {
        scanner.addIncludeFilter(new AnnotationTypeFilter(cls));
      }

      beanDefinitions = scanner.findCandidateComponents(AMBARI_PACKAGE);
    }

    if (null == beanDefinitions || beanDefinitions.size() == 0) {
      LOG.warn("No instances of {} found to register", classes);
      return beanDefinitions;
    }

    Set<com.google.common.util.concurrent.Service> services =
        new HashSet<com.google.common.util.concurrent.Service>();

    for (BeanDefinition beanDefinition : beanDefinitions) {
      String className = beanDefinition.getBeanClassName();
      Class<?> clazz = ClassUtils.resolveClassName(className,
          ClassUtils.getDefaultClassLoader());

      if (null != clazz.getAnnotation(EagerSingleton.class)) {
        bind(clazz).asEagerSingleton();
        LOG.debug("Binding singleton {} eagerly", clazz);
      }

      if (null != clazz.getAnnotation(StaticallyInject.class)) {
        requestStaticInjection(clazz);
        LOG.debug("Statically injecting {} ", clazz);
      }

      // Ambari services are registered with Guava
      if (null != clazz.getAnnotation(AmbariService.class)) {
        // safety check to ensure it's actually a Guava service
        if (!com.google.common.util.concurrent.Service.class.isAssignableFrom(clazz)) {
          String message = MessageFormat.format(
              "Unable to register service {0} because it is not a Service which can be scheduled",
              clazz);

          LOG.warn(message);
          throw new RuntimeException(message);
        }

        // instantiate the service, register as singleton via toInstance()
        com.google.common.util.concurrent.Service service = null;
        try {
          service = (com.google.common.util.concurrent.Service) clazz.newInstance();
          bind((Class<com.google.common.util.concurrent.Service>) clazz).toInstance(service);
          services.add(service);
          LOG.debug("Registering service {} ", clazz);
        } catch (Exception exception) {
          LOG.error("Unable to register {} as a service", clazz, exception);
          throw new RuntimeException(exception);
        }
      }
    }

    ServiceManager manager = new ServiceManager(services);
    bind(ServiceManager.class).toInstance(manager);

    return beanDefinitions;
  }

  /**
   * Searches for all instances of {@link NotificationDispatcher} on the
   * classpath and registers each as a singleton with the
   * {@link DispatchFactory}.
   */
  @SuppressWarnings("unchecked")
  protected Set<BeanDefinition> bindNotificationDispatchers(Set<BeanDefinition> beanDefinitions) {

    // make the factory a singleton
    DispatchFactory dispatchFactory = DispatchFactory.getInstance();
    bind(DispatchFactory.class).toInstance(dispatchFactory);

    if (null == beanDefinitions || beanDefinitions.isEmpty()) {
      ClassPathScanningCandidateComponentProvider scanner =
          new ClassPathScanningCandidateComponentProvider(false);

      // match all implementations of the dispatcher interface
      AssignableTypeFilter filter = new AssignableTypeFilter(
          NotificationDispatcher.class);

      scanner.addIncludeFilter(filter);

      beanDefinitions = scanner.findCandidateComponents("org.apache.ambari.server.notifications.dispatchers");
    }

    // no dispatchers is a problem
    if (null == beanDefinitions || beanDefinitions.size() == 0) {
      LOG.error("No instances of {} found to register", NotificationDispatcher.class);
      return null;
    }

    // for every discovered dispatcher, singleton-ize them and register with
    // the dispatch factory
    for (BeanDefinition beanDefinition : beanDefinitions) {
      String className = beanDefinition.getBeanClassName();
      Class<?> clazz = ClassUtils.resolveClassName(className,
          ClassUtils.getDefaultClassLoader());

      try {
        NotificationDispatcher dispatcher;
        if (clazz.equals(AmbariSNMPDispatcher.class)) {
          dispatcher = (NotificationDispatcher) clazz.getConstructor(Integer.class).newInstance(configuration.getAmbariSNMPUdpBindPort());
        } else if (clazz.equals(SNMPDispatcher.class)) {
          dispatcher = (NotificationDispatcher) clazz.getConstructor(Integer.class).newInstance(configuration.getSNMPUdpBindPort());
        } else {
          dispatcher = (NotificationDispatcher) clazz.newInstance();
        }
        dispatchFactory.register(dispatcher.getType(), dispatcher);
        bind((Class<NotificationDispatcher>) clazz).toInstance(dispatcher);

        LOG.info("Binding and registering notification dispatcher {}", clazz);
      } catch (Exception exception) {
        LOG.error("Unable to bind and register notification dispatcher {}",
            clazz, exception);
      }
    }

    return beanDefinitions;
  }

  /**
   * Searches for all instances of {@link AbstractCheckDescriptor} on the
   * classpath and registers each as a singleton with the
   * {@link UpgradeCheckRegistry}.
   */
  @SuppressWarnings("unchecked")
  protected Set<BeanDefinition> registerUpgradeChecks(Set<BeanDefinition> beanDefinitions) {

    // make the registry a singleton
    UpgradeCheckRegistry registry = new UpgradeCheckRegistry();
    bind(UpgradeCheckRegistry.class).toInstance(registry);

    if (null == beanDefinitions || beanDefinitions.isEmpty()) {
      ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

      // match all implementations of the base check class
      AssignableTypeFilter filter = new AssignableTypeFilter(AbstractCheckDescriptor.class);
      scanner.addIncludeFilter(filter);

      beanDefinitions = scanner.findCandidateComponents(AbstractCheckDescriptor.class.getPackage().getName());
    }

    // no dispatchers is a problem
    if (null == beanDefinitions || beanDefinitions.size() == 0) {
      LOG.error("No instances of {} found to register", AbstractCheckDescriptor.class);
      return null;
    }

    // for every discovered check, singleton-ize them and register with the
    // registry
    for (BeanDefinition beanDefinition : beanDefinitions) {
      String className = beanDefinition.getBeanClassName();
      Class<?> clazz = ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader());

      try {
        AbstractCheckDescriptor upgradeCheck = (AbstractCheckDescriptor) clazz.newInstance();
        bind((Class<AbstractCheckDescriptor>) clazz).toInstance(upgradeCheck);
        registry.register(upgradeCheck);
      } catch (Exception exception) {
        LOG.error("Unable to bind and register upgrade check {}", clazz, exception);
      }
    }

    // log the order of the pre-upgrade checks
    List<AbstractCheckDescriptor> upgradeChecks = registry.getUpgradeChecks();
    for (AbstractCheckDescriptor upgradeCheck : upgradeChecks) {
      LOG.debug("Registered pre-upgrade check {}", upgradeCheck.getClass());
    }
    return beanDefinitions;
  }
}
