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
package org.apache.ambari.resource.statemachine;

import org.apache.ambari.event.AsyncDispatcher;
import org.apache.ambari.event.Dispatcher;
import org.apache.ambari.event.EventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class StateMachineInvoker implements StateMachineInvokerInterface {
  
  private Dispatcher dispatcher;
  
  @Inject
  StateMachineInvoker() {
    dispatcher = new AsyncDispatcher();
    dispatcher.register(ClusterEventType.class, new ClusterEventDispatcher());
    dispatcher.register(ServiceEventType.class, new ServiceEventDispatcher());
    dispatcher.register(RoleEventType.class, new RoleEventDispatcher());
    dispatcher.start();
  }
  

  public EventHandler getAMBARIEventHandler() {
    return dispatcher.getEventHandler();
  }

  private static class ClusterEventDispatcher 
  implements EventHandler<ClusterEvent> {
    @Override
    public void handle(ClusterEvent event) {
      ((EventHandler<ClusterEvent>)event.getCluster()).handle(event);
    }
  }
  
  private static class ServiceEventDispatcher 
  implements EventHandler<ServiceEvent> {
    @Override
    public void handle(ServiceEvent event) {
      ((EventHandler<ServiceEvent>)event.getService()).handle(event);
    }
  }
  
  private static class RoleEventDispatcher 
  implements EventHandler<RoleEvent> {
    @Override
    public void handle(RoleEvent event) {
      ((EventHandler<RoleEvent>)event.getRole()).handle(event);
    }
  }  
}
