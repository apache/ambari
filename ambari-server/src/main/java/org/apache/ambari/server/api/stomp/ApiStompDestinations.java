/*
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
package org.apache.ambari.server.api.stomp;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.events.NamedTaskUpdateEvent;
import org.apache.ambari.server.events.STOMPEvent;

final class ApiStompDestinations {
  static final String EVENT_PREFIX = "/events/";
  static final String TASK_PREFIX = "/events/tasks/";

  private static final Map<STOMPEvent.Type, String> DESTINATIONS;
  private static final Set<String> BROADCAST_DESTINATIONS;

  static {
    Map<STOMPEvent.Type, String> destinations = new HashMap<>();
    destinations.put(STOMPEvent.Type.ALERT, "/events/alerts");
    destinations.put(STOMPEvent.Type.ALERT_GROUP, "/events/alert_group");
    destinations.put(STOMPEvent.Type.UI_TOPOLOGY, "/events/ui_topologies");
    destinations.put(STOMPEvent.Type.CONFIGS, "/events/configs");
    destinations.put(STOMPEvent.Type.HOSTCOMPONENT, "/events/hostcomponents");
    destinations.put(STOMPEvent.Type.REQUEST, "/events/requests");
    destinations.put(STOMPEvent.Type.SERVICE, "/events/services");
    destinations.put(STOMPEvent.Type.HOST, "/events/hosts");
    destinations.put(STOMPEvent.Type.UI_ALERT_DEFINITIONS, "/events/alert_definitions");
    destinations.put(STOMPEvent.Type.UPGRADE, "/events/upgrade");
    DESTINATIONS = Collections.unmodifiableMap(destinations);
    BROADCAST_DESTINATIONS = Collections.unmodifiableSet(new HashSet<>(destinations.values()));
  }

  private ApiStompDestinations() {
  }

  static boolean isAllowedSubscription(String destination) {
    return BROADCAST_DESTINATIONS.contains(destination) || taskId(destination).isPresent();
  }

  static boolean isEventDestination(String destination) {
    return destination != null && destination.startsWith(EVENT_PREFIX);
  }

  static Optional<Long> taskId(String destination) {
    if (destination == null || !destination.startsWith(TASK_PREFIX)) {
      return Optional.empty();
    }

    String id = destination.substring(TASK_PREFIX.length());
    if (id.isEmpty()) {
      return Optional.empty();
    }
    for (int i = 0; i < id.length(); i++) {
      if (id.charAt(i) < '0' || id.charAt(i) > '9') {
        return Optional.empty();
      }
    }

    try {
      return Optional.of(Long.parseLong(id));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  static Optional<String> destinationFor(STOMPEvent event) {
    if (event == null) {
      return Optional.empty();
    }
    if (event.getType() == STOMPEvent.Type.NAMEDTASK) {
      if (!(event instanceof NamedTaskUpdateEvent) || ((NamedTaskUpdateEvent) event).getId() == null) {
        return Optional.empty();
      }
      return Optional.of(TASK_PREFIX + ((NamedTaskUpdateEvent) event).getId());
    }
    return Optional.ofNullable(DESTINATIONS.get(event.getType()));
  }
}
