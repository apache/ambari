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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.ambari.server.events.HostComponentUpdate;
import org.apache.ambari.server.events.HostComponentsUpdateEvent;

import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;

@Singleton
public class HostComponentUpdateEventPublisher extends BufferedUpdateEventPublisher<HostComponentsUpdateEvent> {

  @Override
  protected Runnable getScheduledPublisher(EventBus m_eventBus) {
    return new HostComponentsEventRunnable(m_eventBus);
  }

  private class HostComponentsEventRunnable implements Runnable {

    private final EventBus eventBus;

    public HostComponentsEventRunnable(EventBus eventBus) {
      this.eventBus = eventBus;
    }

    @Override
    public void run() {
      List<HostComponentsUpdateEvent> hostComponentUpdateEvents = retrieveBuffer();
      if (hostComponentUpdateEvents.isEmpty()) {
        return;
      }
      List<HostComponentUpdate> hostComponentUpdates = hostComponentUpdateEvents.stream().flatMap(
          u -> u.getHostComponentUpdates().stream()).collect(Collectors.toList());

      HostComponentsUpdateEvent resultEvents = new HostComponentsUpdateEvent(hostComponentUpdates);
      //TODO add logging and metrics posting
      eventBus.post(resultEvents);
    }
  }
}
