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
import com.google.inject.persist.jpa.JpaPersistModule;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBInMemoryImpl;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;

import java.util.Properties;

/**
 * Used for injection purposes.
 *
 */
public class ControllerModule extends AbstractModule {

  private final Configuration configuration;

  public ControllerModule() {
    configuration = new Configuration();
  }

  public ControllerModule(Properties properties) {
    configuration = new Configuration(properties);
  }

  @Override
  protected void configure() {
    bind(Configuration.class).toInstance(configuration);

    install(new JpaPersistModule(configuration.getPersistenceType().getUnitName()));

    install(new FactoryModuleBuilder().implement(Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
    install(new FactoryModuleBuilder().implement(Host.class, HostImpl.class).build(HostFactory.class));
    install(new FactoryModuleBuilder().implement(Service.class, ServiceImpl.class).build(ServiceFactory.class));
    install(new FactoryModuleBuilder().implement(ServiceComponent.class, ServiceComponentImpl.class).build(ServiceComponentFactory.class));
    install(new FactoryModuleBuilder().implement(ServiceComponentHost.class, ServiceComponentHostImpl.class).build(ServiceComponentHostFactory.class));

    bind(Gson.class).in(Scopes.SINGLETON);
    bind(Clusters.class).to(ClustersImpl.class);
    bind(ActionDBAccessor.class).to(ActionDBInMemoryImpl.class);
    bindConstant().annotatedWith(Names.named("schedulerSleeptime")).to(10000L);
    bindConstant().annotatedWith(Names.named("actionTimeout")).to(60000L);
    bind(AmbariManagementController.class)
        .to(AmbariManagementControllerImpl.class);
  }
}
