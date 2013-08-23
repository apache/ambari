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

import org.apache.ambari.server.state.svccomphost.HBaseMasterPortScanner;
import com.google.gson.Gson;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.persist.jpa.JpaPersistModule;
import org.apache.ambari.server.actionmanager.*;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.serveraction.ServerActionManager;
import org.apache.ambari.server.serveraction.ServerActionManagerImpl;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

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
    bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(10000L);
    bindConstant().annotatedWith(Names.named("actionTimeout")).to(600000L);
    bind(AmbariManagementController.class)
        .to(AmbariManagementControllerImpl.class);
    bind(HBaseMasterPortScanner.class).in(Singleton.class);
    bind(ServerActionManager.class).to(ServerActionManagerImpl.class);
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
    install(new FactoryModuleBuilder().build(StageFactory.class));
    install(new FactoryModuleBuilder().build(HostRoleCommandFactory.class));
  }

}
