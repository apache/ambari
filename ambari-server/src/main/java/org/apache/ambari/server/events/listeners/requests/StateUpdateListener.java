/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events.listeners.requests;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.events.AmbariUpdateEvent;
import org.apache.ambari.server.events.DefaultMessageEmitter;
import org.apache.ambari.server.events.publishers.StateUpdateEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;

public class StateUpdateListener {
  private final AgentSessionManager agentSessionManager;

  @Autowired
  private DefaultMessageEmitter defaultMessageEmitter;

  public StateUpdateListener(Injector injector) {
    StateUpdateEventPublisher stateUpdateEventPublisher =
      injector.getInstance(StateUpdateEventPublisher.class);
    agentSessionManager = injector.getInstance(AgentSessionManager.class);
    stateUpdateEventPublisher.register(this);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onUpdateEvent(AmbariUpdateEvent event) throws AmbariException {
    defaultMessageEmitter.emitMessage(event);
  }
}
