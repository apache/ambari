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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;

/**
 * The {@link MockEventListener} is used to provide a way to capture events
 * being fired via an {@link EventBus}.
 */
@Singleton
public class MockEventListener {

  /**
   * When an event is received, its class is captured and the event object is
   * added to the list.
   */
  private final Map<Class<?>, List<Object>> m_receivedEvents = new HashMap<Class<?>, List<Object>>();

  /**
   * Resets the captured events.
   */
  public void reset() {
    m_receivedEvents.clear();
  }

  /**
   * Gets whether an event of the specified class was received.
   *
   * @param clazz
   * @return
   */
  public boolean isEventReceived(Class<?> clazz) {
    if (!m_receivedEvents.containsKey(clazz)) {
      return false;
    }

    return m_receivedEvents.get(clazz).size() > 0;
  }

  /**
   * Gets the total number of events received for the specified class.
   *
   * @param clazz
   * @return
   */
  public int getEventReceivedCount(Class<?> clazz){
    if (!m_receivedEvents.containsKey(clazz)) {
      return 0;
    }

    return m_receivedEvents.get(clazz).size();
  }

  /**
   * @param event
   */
  @Subscribe
  public void onEvent(MaintenanceModeEvent event) {
    handleEvent(event);
  }

  /**
   * Inserts the event into the map of class to event invocations.
   *
   * @param event
   */
  private void handleEvent(Object event) {
    List<Object> events = m_receivedEvents.get(event.getClass());
    if (null == events) {
      events = new ArrayList<Object>();
      m_receivedEvents.put(event.getClass(), events);
    }

    events.add(event);
  }
}
