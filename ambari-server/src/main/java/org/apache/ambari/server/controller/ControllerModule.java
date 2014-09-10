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
import static org.eclipse.persistence.config.PersistenceUnitProperties.THROW_EXCEPTIONS;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.MemberResourceProvider;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.scheduler.ExecutionSchedulerImpl;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.SecurityHelperImpl;
import org.apache.ambari.server.serveraction.ServerActionManager;
import org.apache.ambari.server.serveraction.ServerActionManagerImpl;
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
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionImpl;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.view.ViewInstanceHandlerList;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.jpa.AmbariJpaPersistModule;

/**
 * Used for injection purposes.
 */
public class ControllerModule extends AbstractModule {

  private final Configuration configuration;
  private final HostsMap hostsMap;
  private boolean dbInitNeeded;
  private final Gson prettyGson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();


  // ----- Constructors ------------------------------------------------------

  public ControllerModule() throws Exception {
    configuration = new Configuration();
    hostsMap = new HostsMap(configuration);
  }

  public ControllerModule(Properties properties) throws Exception {
    configuration = new Configuration(properties);
    hostsMap = new HostsMap(configuration);
  }


  // ----- ControllerModule --------------------------------------------------

  /**
   * Get the common persistence related configuration properties.
   *
   * @return the configuration properties
   */
  public static Properties getPersistenceProperties(Configuration configuration) {
    Properties properties = new Properties();

    // custom jdbc properties
    Map<String, String> custom = configuration.getDatabaseCustomProperties();

    if (0 != custom.size()) {
      for (Entry<String, String> entry : custom.entrySet()) {
        properties.setProperty("eclipselink.jdbc.property." + entry.getKey(),
            entry.getValue());
      }
    }

    switch (configuration.getPersistenceType()) {
      case IN_MEMORY:
        properties.setProperty(JDBC_URL, Configuration.JDBC_IN_MEMORY_URL);
        properties.setProperty(JDBC_DRIVER, Configuration.JDBC_IN_MEMROY_DRIVER);
        properties.setProperty(DDL_GENERATION, DROP_AND_CREATE);
        properties.setProperty(THROW_EXCEPTIONS, "true");
      case REMOTE:
        properties.setProperty(JDBC_URL, configuration.getDatabaseUrl());
        properties.setProperty(JDBC_DRIVER, configuration.getDatabaseDriver());
        break;
      case LOCAL:
        properties.setProperty(JDBC_URL, configuration.getLocalDatabaseUrl());
        properties.setProperty(JDBC_DRIVER, Configuration.JDBC_LOCAL_DRIVER);
        break;
    }
    return properties;
  }


  // ----- AbstractModule ----------------------------------------------------

  @Override
  protected void configure() {
    installFactories();

    final SessionIdManager sessionIdManager = new HashSessionIdManager();
    final SessionManager sessionManager = new HashSessionManager();
    sessionManager.setSessionPath("/");
    sessionManager.setSessionIdManager(sessionIdManager);
    bind(SessionManager.class).toInstance(sessionManager);
    bind(SessionIdManager.class).toInstance(sessionIdManager);

    bind(Configuration.class).toInstance(configuration);
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
    bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(10000L);

    // This time is added to summary timeout time of all tasks in stage
    // So it's an "additional time", given to stage to finish execution before
    // it is considered as timed out
    bindConstant().annotatedWith(Names.named("actionTimeout")).to(600000L);

    bindConstant().annotatedWith(Names.named("dbInitNeeded")).to(dbInitNeeded);
    bindConstant().annotatedWith(Names.named("statusCheckInterval")).to(5000L);

    //ExecutionCommands cache size

    bindConstant().annotatedWith(Names.named("executionCommandCacheSize")).
        to(configuration.getExecutionCommandsCacheSize());

    bind(AmbariManagementController.class)
        .to(AmbariManagementControllerImpl.class);
    bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
    bind(ServerActionManager.class).to(ServerActionManagerImpl.class);
    bind(ExecutionScheduler.class).to(ExecutionSchedulerImpl.class);
    bind(DBAccessor.class).to(DBAccessorImpl.class);
    bind(ViewInstanceHandlerList.class).to(AmbariHandlerList.class);

    requestStaticInjection(ExecutionCommandWrapper.class);
  }


  // ----- helper methods ----------------------------------------------------

  private PersistModule buildJpaPersistModule() {
    PersistenceType persistenceType = configuration.getPersistenceType();
    AmbariJpaPersistModule jpaPersistModule = new AmbariJpaPersistModule(Configuration.JDBC_UNIT_NAME);

    Properties persistenceProperties = getPersistenceProperties(configuration);

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
    install(new FactoryModuleBuilder().build(StageFactory.class));
    install(new FactoryModuleBuilder().build(RequestFactory.class));

    bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
    bind(SecurityHelper.class).toInstance(SecurityHelperImpl.getInstance());
  }
}
