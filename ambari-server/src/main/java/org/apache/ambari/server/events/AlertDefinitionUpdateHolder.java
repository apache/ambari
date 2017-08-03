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

package org.apache.ambari.server.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.events.publishers.StateUpdateEventPublisher;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AlertDefinitionUpdateHolder {
  private Map<Long, AlertDefinitionsUpdateEvent> cachedEvents = new ConcurrentHashMap<>();

  @Inject
  private StateUpdateEventPublisher stateUpdateEventPublisher;

  public void updateIfNeeded(AlertDefinitionsUpdateEvent event) {
    if (cachedEvents.containsKey(event.getDefinitionId())) {
      AlertDefinitionsUpdateEvent cachedUpdate = cachedEvents.get(event.getDefinitionId());
      AlertDefinitionsUpdateEvent updateEvent = new AlertDefinitionsUpdateEvent(event.getDefinitionId());
      boolean updated = false;

      if (event.getClusterId() != cachedUpdate.getClusterId()) {
        cachedUpdate.setClusterId(event.getClusterId());
        updateEvent.setClusterId(event.getClusterId());
        updated = true;
      }
      if (!StringUtils.equals(event.getComponentName(), cachedUpdate.getComponentName())) {
        cachedUpdate.setComponentName(event.getComponentName());
        updateEvent.setComponentName(event.getComponentName());
        updated = true;
      }
      if (!StringUtils.equals(event.getDescription(), cachedUpdate.getDescription())) {
        cachedUpdate.setDescription(event.getDescription());
        updateEvent.setDescription(event.getDescription());
        updated = true;
      }
      if (!StringUtils.equals(event.getHelpURL(), cachedUpdate.getHelpURL())) {
        cachedUpdate.setHelpURL(event.getHelpURL());
        updateEvent.setHelpURL(event.getHelpURL());
        updated = true;
      }
      if (event.getInterval() != cachedUpdate.getInterval()) {
        cachedUpdate.setInterval(event.getInterval());
        updateEvent.setInterval(event.getInterval());
        updated = true;
      }
      if (!StringUtils.equals(event.getLabel(), cachedUpdate.getLabel())) {
        cachedUpdate.setLabel(event.getLabel());
        updateEvent.setLabel(event.getLabel());
        updated = true;
      }
      if (!StringUtils.equals(event.getName(), cachedUpdate.getName())) {
        cachedUpdate.setName(event.getName());
        updateEvent.setName(event.getName());
        updated = true;
      }
      if (event.getRepeatTolerance() != cachedUpdate.getRepeatTolerance()) {
        cachedUpdate.setRepeatTolerance(event.getRepeatTolerance());
        updateEvent.setRepeatTolerance(event.getRepeatTolerance());
        updated = true;
      }
      if (!event.getScope().equals(cachedUpdate.getScope())) {
        cachedUpdate.setScope(event.getScope());
        updateEvent.setScope(event.getScope());
        updated = true;
      }
      if (!StringUtils.equals(event.getServiceName(), cachedUpdate.getServiceName())) {
        cachedUpdate.setServiceName(event.getServiceName());
        updateEvent.setServiceName(event.getServiceName());
        updated = true;
      }
      if (!event.getSource().equals(cachedUpdate.getSource())) {
        cachedUpdate.setSource(event.getSource());
        updateEvent.setSource(event.getSource());
        updated = true;
      }
      if (!event.getEnabled().equals(cachedUpdate.getEnabled())) {
        cachedUpdate.setEnabled(event.getEnabled());
        updateEvent.setEnabled(event.getEnabled());
        updated = true;
      }
      if (!event.getRepeatToleranceEnabled().equals(cachedUpdate.getRepeatToleranceEnabled())) {
        cachedUpdate.setRepeatToleranceEnabled(event.getRepeatToleranceEnabled());
        updateEvent.setRepeatToleranceEnabled(event.getRepeatToleranceEnabled());
        updated = true;
      }
      if (updated) {
        stateUpdateEventPublisher.publish(updateEvent);
      }
    } else {
      cachedEvents.put(event.getDefinitionId(), event);
      stateUpdateEventPublisher.publish(event);
    }
  }
}
