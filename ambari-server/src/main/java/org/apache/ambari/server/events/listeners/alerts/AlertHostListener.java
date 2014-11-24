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
package org.apache.ambari.server.events.listeners.alerts;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.HostRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AlertHostListener} class handles {@link HostRemovedEvent} and
 * ensures that {@link AlertCurrentEntity} instances are properly cleaned up
 */
@Singleton
@EagerSingleton
public class AlertHostListener {
  /**
   * Logger.
   */
  private static Log LOG = LogFactory.getLog(AlertHostListener.class);

  /**
   * Used for removing current alerts when a service is removed.
   */
  @Inject
  private AlertsDAO m_alertsDao;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertHostListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Removes any current alerts associated with the specified host.
   *
   * @param event
   *          the published event being handled (not {@code null}).
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(HostRemovedEvent event) {
    LOG.debug(event);

    // remove any current alerts for the removed host
    m_alertsDao.removeCurrentByHost(event.getHostName());
  }
}
