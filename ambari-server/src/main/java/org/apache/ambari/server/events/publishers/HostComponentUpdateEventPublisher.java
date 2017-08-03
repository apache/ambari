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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.events.HostComponentUpdate;
import org.apache.ambari.server.events.HostComponentsUpdateEvent;

import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;

@Singleton
public class HostComponentUpdateEventPublisher {

  private final Long TIMEOUT = 1000L;
  private AtomicLong previousTime = new AtomicLong(0);
  private AtomicBoolean collecting = new AtomicBoolean(false);
  private ConcurrentLinkedQueue<HostComponentUpdate> buffer = new ConcurrentLinkedQueue<>();
  private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

  public void publish(HostComponentsUpdateEvent event, EventBus m_eventBus) {
    Long eventTime = System.currentTimeMillis();
    if (eventTime - previousTime.get() <= TIMEOUT && !collecting.get()) {
      buffer.addAll(event.getHostComponentUpdates());
      collecting.set(true);
      scheduledExecutorService.schedule(new HostComponentsEventRunnable(m_eventBus),
          TIMEOUT, TimeUnit.MILLISECONDS);
    } else if (collecting.get()) {
      buffer.addAll(event.getHostComponentUpdates());
    } else {
      //TODO add logging and metrics posting
      previousTime.set(eventTime);
      m_eventBus.post(event);
    }
  }

  private class HostComponentsEventRunnable implements Runnable {

    private final EventBus eventBus;

    public HostComponentsEventRunnable(EventBus eventBus) {
      this.eventBus = eventBus;
    }

    @Override
    public void run() {
      List<HostComponentUpdate> hostComponentUpdates = new ArrayList<>();
      while (!buffer.isEmpty()) {
        hostComponentUpdates.add(buffer.poll());
      }
      HostComponentsUpdateEvent resultEvents = new HostComponentsUpdateEvent(hostComponentUpdates);
      //TODO add logging and metrics posting
      eventBus.post(resultEvents);
      previousTime.set(System.currentTimeMillis());
      collecting.set(false);
    }
  }
}
