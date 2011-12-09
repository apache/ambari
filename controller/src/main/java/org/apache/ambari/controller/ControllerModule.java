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
package org.apache.ambari.controller;

import org.apache.ambari.components.ComponentModule;
import org.apache.ambari.controller.rest.agent.ControllerResource;
import org.apache.ambari.controller.rest.resources.ClustersResource;
import org.apache.ambari.controller.rest.resources.NodesResource;
import org.apache.ambari.controller.rest.resources.StacksResource;
import org.apache.ambari.datastore.PersistentDataStore;
import org.apache.ambari.datastore.impl.ZookeeperDS;
import org.apache.ambari.resource.statemachine.StateMachineInvoker;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class ControllerModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new ComponentModule());
    bind(PersistentDataStore.class).to(ZookeeperDS.class);
    requestStaticInjection(ClustersResource.class,
                           NodesResource.class,
                           StacksResource.class,
                           ControllerResource.class,
                           StateMachineInvoker.class);
    install(new FactoryModuleBuilder()
              .implement(Cluster.class,Cluster.class)
              .build(ClusterFactory.class));
  }

}
