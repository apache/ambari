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
package org.apache.ambari.server.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.MessageDestinationIsNotDefinedException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class DefaultMessageEmitter extends MessageEmitter {
  private final Map<AmbariUpdateEvent.Type, String> DEFAULT_DESTINATIONS =
      Collections.unmodifiableMap(new HashMap<AmbariUpdateEvent.Type, String>(){{
        put(AmbariUpdateEvent.Type.ALERT, "/events/alerts");
        put(AmbariUpdateEvent.Type.METADATA, "/events/metadata");
        put(AmbariUpdateEvent.Type.HOSTLEVELPARAMS, "/host_level_params");
        put(AmbariUpdateEvent.Type.UI_TOPOLOGY, "/events/ui_topologies");
        put(AmbariUpdateEvent.Type.AGENT_TOPOLOGY, "/events/topologies");
        put(AmbariUpdateEvent.Type.AGENT_CONFIGS, "/configs");
        put(AmbariUpdateEvent.Type.CONFIGS, "/events/configs");
        put(AmbariUpdateEvent.Type.HOSTCOMPONENT, "/events/hostcomponents");
        put(AmbariUpdateEvent.Type.REQUEST, "/events/requests");
        put(AmbariUpdateEvent.Type.SERVICE, "/events/services");
        put(AmbariUpdateEvent.Type.HOST, "/events/hosts");
        put(AmbariUpdateEvent.Type.COMMAND, "/commands");
        put(AmbariUpdateEvent.Type.ALERT_DEFINITIONS, "/alert_definitions");
        put(AmbariUpdateEvent.Type.UI_ALERT_DEFINITIONS, "/events/alert_definitions");
  }});

  public DefaultMessageEmitter(AgentSessionManager agentSessionManager, SimpMessagingTemplate simpMessagingTemplate) {
    super(agentSessionManager, simpMessagingTemplate);
  }

  @Override
  public void emitMessage(AmbariUpdateEvent event) throws AmbariException {
    String destination = DEFAULT_DESTINATIONS.get(event.getType());
    if (destination == null) {
      throw new MessageDestinationIsNotDefinedException(event.getType());
    }
    if (event instanceof AmbariHostUpdateEvent) {
      AmbariHostUpdateEvent hostUpdateEvent = (AmbariHostUpdateEvent) event;
      emitMessageToHost(hostUpdateEvent, destination);
    } else {
      emitMessageToAll(event, destination);
    }
  }
}
