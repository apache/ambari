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

package org.apache.ambari.server.events.publishers;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.events.ServiceUpdateEvent;

import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;

@Singleton
public class ServiceUpdateEventPublisher extends BufferedUpdateEventPublisher<ServiceUpdateEvent> {

  @Override
  protected Runnable getScheduledPublisher(EventBus m_eventBus) {
    return new ServiceEventRunnable(m_eventBus);
  }

  private class ServiceEventRunnable implements Runnable {

    private final EventBus eventBus;

    public ServiceEventRunnable(EventBus eventBus) {
      this.eventBus = eventBus;
    }

    @Override
    public void run() {
      List<ServiceUpdateEvent> serviceUpdates = retrieveBuffer();
      if (serviceUpdates.isEmpty()) {
        return;
      }
      List<ServiceUpdateEvent> filtered = new ArrayList<>();
      for (ServiceUpdateEvent event : serviceUpdates) {
        int pos = filtered.indexOf(event);
        if (pos != -1) {
          if (event.getState() != null) {
            filtered.get(pos).setState(event.getState());
          }
          if (event.getMaintenanceState() != null) {
            filtered.get(pos).setMaintenanceState(event.getMaintenanceState());
          }
        } else {
          filtered.add(event);
        }
      }
      for (ServiceUpdateEvent serviceUpdateEvent : serviceUpdates) {
        eventBus.post(serviceUpdateEvent);
      }
    }
  }
}
