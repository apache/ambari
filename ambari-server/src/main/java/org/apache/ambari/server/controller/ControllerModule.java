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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.CustomActionDBAccessor;
import org.apache.ambari.server.actionmanager.CustomActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.PersistenceType;
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
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.persist.jpa.JpaPersistModule;

/**
 * Used for injection purposes.
 */
public class ControllerModule extends AbstractModule {

  private final Configuration configuration;
  private final HostsMap hostsMap;

  public ControllerModule() throws Exception {
    configuration = new Configuration();
    hostsMap = new HostsMap(configuration);
  }

  public ControllerModule(Properties properties) throws Exception {
    configuration = new Configuration(properties);
    hostsMap = new HostsMap(configuration);
  }

  @Override
  protected void configure() {
    installFactories();

    bind(Configuration.class).toInstance(configuration);
    bind(HostsMap.class).toInstance(hostsMap);
    bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());

    install(buildJpaPersistModule());

    bind(Gson.class).in(Scopes.SINGLETON);
    bind(Clusters.class).to(ClustersImpl.class);
    bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
    bind(CustomActionDBAccessor.class).to(CustomActionDBAccessorImpl.class);
    bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(10000L);
    bindConstant().annotatedWith(Names.named("actionTimeout")).to(600000L);

    //ExecutionCommands cache size

    bindConstant().annotatedWith(Names.named("executionCommandCacheSize")).
        to(configuration.getExecutionCommandsCacheSize());

    bind(AmbariManagementController.class)
        .to(AmbariManagementControllerImpl.class);
    bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
    bind(ServerActionManager.class).to(ServerActionManagerImpl.class);

    requestStaticInjection(ExecutionCommandWrapper.class);
  }

  private JpaPersistModule buildJpaPersistModule() {
    PersistenceType persistenceType = configuration.getPersistenceType();
    JpaPersistModule jpaPersistModule = new JpaPersistModule(Configuration.JDBC_UNIT_NAME);

    Properties properties = new Properties();

    // custom jdbc properties
    Map<String, String> custom = configuration.getDatabaseCustomProperties();
    
    if (0 != custom.size()) {
      for (Entry<String, String> entry : custom.entrySet()) {
        properties.setProperty("eclipselink.jdbc.property." + entry.getKey(),
           entry.getValue());
      }
    }    

    switch (persistenceType) {
      case IN_MEMORY:
        properties.put("javax.persistence.jdbc.url", Configuration.JDBC_IN_MEMORY_URL);
        properties.put("javax.persistence.jdbc.driver", Configuration.JDBC_IN_MEMROY_DRIVER);
        properties.put("eclipselink.ddl-generation", "drop-and-create-tables");
        properties.put("eclipselink.orm.throw.exceptions", "true");
        jpaPersistModule.properties(properties);
        return jpaPersistModule;
      case REMOTE:
        properties.put("javax.persistence.jdbc.url", configuration.getDatabaseUrl());
        properties.put("javax.persistence.jdbc.driver", configuration.getDatabaseDriver());
        break;
      case LOCAL:
        properties.put("javax.persistence.jdbc.url", configuration.getLocalDatabaseUrl());
        properties.put("javax.persistence.jdbc.driver", Configuration.JDBC_LOCAL_DRIVER);
        break;
    }

    properties.setProperty("javax.persistence.jdbc.user", configuration.getDatabaseUser());
    properties.setProperty("javax.persistence.jdbc.password", configuration.getDatabasePassword());

    switch (configuration.getJPATableGenerationStrategy()) {
      case CREATE:
        properties.setProperty("eclipselink.ddl-generation", "create-tables");
        break;
      case DROP_AND_CREATE:
        properties.setProperty("eclipselink.ddl-generation", "drop-and-create-tables");
        break;
      default:
        break;
    }
    properties.setProperty("eclipselink.ddl-generation.output-mode", "both");
    properties.setProperty("eclipselink.create-ddl-jdbc-file-name", "DDL-create.jdbc");
    properties.setProperty("eclipselink.drop-ddl-jdbc-file-name", "DDL-drop.jdbc");

    jpaPersistModule.properties(properties);

    return jpaPersistModule;
  }

  private void installFactories() {
    install(new FactoryModuleBuilder().implement(
        Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
    install(new FactoryModuleBuilder().implement(
        Host.class, HostImpl.class).build(HostFactory.class));
    install(new FactoryModuleBuilder().implement(
        Service.class, ServiceImpl.class).build(ServiceFactory.class));
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
    install(new FactoryModuleBuilder().build(StageFactory.class));
    bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
  }

}
