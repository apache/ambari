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

import org.apache.ambari.server.HostNotRegisteredException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class AlertDefinitionsMessageEmitter extends MessageEmitter {

  private final String ALERT_DESTINATION_TO_HOST = "/alert_definitions";
  private final String ALERT_DESTINATION_TO_API = "/events/alert_definitions";

  public AlertDefinitionsMessageEmitter(AgentSessionManager agentSessionManager, SimpMessagingTemplate simpMessagingTemplate) {
    super(agentSessionManager, simpMessagingTemplate);
  }

  @Override
  public void emitMessage(AmbariUpdateEvent event) throws HostNotRegisteredException {
    emitMessageToHost((AmbariHostUpdateEvent) event, ALERT_DESTINATION_TO_HOST);
    emitMessageToAll(event, ALERT_DESTINATION_TO_API);
  }
}
