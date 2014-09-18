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
package org.apache.ambari.server.events.listeners;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.NotificationState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AlertStateChangedListener} class response to
 * {@link AlertStateChangeEvent} and updates {@link AlertNoticeEntity} instances
 * in the database.
 */
@Singleton
public class AlertStateChangedListener {

  /**
   * Logger.
   */
  private static Log LOG = LogFactory.getLog(AlertStateChangedListener.class);

  /**
   * Used for looking up groups and targets.
   */
  @Inject
  private AlertDispatchDAO m_alertsDispatchDao;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertStateChangedListener(AlertEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Listens for when an alert's state has changed.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAlertEvent(AlertStateChangeEvent event) {
    LOG.debug(event);

    AlertHistoryEntity history = event.getNewHistoricalEntry();
    AlertDefinitionEntity definition = history.getAlertDefinition();

    List<AlertGroupEntity> groups = m_alertsDispatchDao.findGroupsByDefinition(definition);

    // for each group, determine if there are any targets that need to receive
    // a notification about the alert state change event
    for (AlertGroupEntity group : groups) {
      Set<AlertTargetEntity> targets = group.getAlertTargets();
      if (null == targets || targets.size() == 0) {
        continue;
      }

      for (AlertTargetEntity target : targets) {
        AlertNoticeEntity notice = new AlertNoticeEntity();
        notice.setUuid(UUID.randomUUID().toString());
        notice.setAlertTarget(target);
        notice.setAlertHistory(event.getNewHistoricalEntry());
        notice.setNotifyState(NotificationState.PENDING);

        m_alertsDispatchDao.create(notice);
      }
    }
  }
}
