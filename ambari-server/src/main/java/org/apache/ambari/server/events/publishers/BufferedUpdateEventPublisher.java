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

import com.google.common.eventbus.EventBus;
import com.google.inject.Singleton;

@Singleton
public abstract class BufferedUpdateEventPublisher<T> {

  private static final long TIMEOUT = 1000L;
  private final AtomicLong previousTime = new AtomicLong(0);
  private final AtomicBoolean collecting = new AtomicBoolean(false);
  private final ConcurrentLinkedQueue<T> buffer = new ConcurrentLinkedQueue<>();
  private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

  public void publish(T event, EventBus m_eventBus) {
    long eventTime = System.currentTimeMillis();
    if ((eventTime - previousTime.get() <= TIMEOUT) && !collecting.get()) {
      buffer.add(event);
      collecting.set(true);
      scheduledExecutorService.schedule(getScheduledPublisher(m_eventBus),
          TIMEOUT, TimeUnit.MILLISECONDS);
    } else if (collecting.get()) {
      buffer.add(event);
    } else {
      //TODO add logging and metrics posting
      previousTime.set(eventTime);
      m_eventBus.post(event);
    }
  }

  protected abstract Runnable getScheduledPublisher(EventBus m_eventBus);

  protected List<T> retrieveBuffer() {
    resetCollecting();
    List<T> bufferContent = new ArrayList<>();
    while (!buffer.isEmpty()) {
      bufferContent.add(buffer.poll());
    }
    return bufferContent;
  }

  protected void resetCollecting() {
    previousTime.set(System.currentTimeMillis());
    collecting.set(false);
  }
}
