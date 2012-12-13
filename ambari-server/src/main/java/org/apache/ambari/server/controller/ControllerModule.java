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
import com.google.gson.Gson;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.jpa.JpaPersistModule;
import org.apache.ambari.server.actionmanager.*;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.dao.ClearEntityManagerInterceptor;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import java.util.Properties;

/**
 * Used for injection purposes.
 *
 */
public class ControllerModule extends AbstractModule {

  private final Configuration configuration;
  private final AmbariMetaInfo ambariMetaInfo;

  public ControllerModule() throws Exception {
    configuration = new Configuration();
    ambariMetaInfo = new AmbariMetaInfo(configuration);
  }

  public ControllerModule(Properties properties) throws Exception {
    configuration = new Configuration(properties);
    ambariMetaInfo = new AmbariMetaInfo(configuration);
  }

  @Override
  protected void configure() {
    bindInterceptors();
    installFactories();

    bind(Configuration.class).toInstance(configuration);
    bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);

    bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());

    JpaPersistModule jpaPersistModule = new JpaPersistModule(configuration.getPersistenceType().getUnitName());
    if (configuration.getPersistenceType() == PersistenceType.POSTGRES) {
      Properties properties = new Properties();
      properties.setProperty("javax.persistence.jdbc.user", configuration.getDatabaseUser());
      properties.setProperty("javax.persistence.jdbc.password", configuration.getDatabasePassword());
      jpaPersistModule.properties(properties);
    }

    install(jpaPersistModule);


    bind(Gson.class).in(Scopes.SINGLETON);
    bind(Clusters.class).to(ClustersImpl.class);
    bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
    bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(10000L);
    bindConstant().annotatedWith(Names.named("actionTimeout")).to(300000L);
    bind(AmbariManagementController.class)
        .to(AmbariManagementControllerImpl.class);
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

  private void bindInterceptors() {
    ClearEntityManagerInterceptor clearEntityManagerInterceptor = new ClearEntityManagerInterceptor();
    requestInjection(clearEntityManagerInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Transactional.class), clearEntityManagerInterceptor);

  }
}
