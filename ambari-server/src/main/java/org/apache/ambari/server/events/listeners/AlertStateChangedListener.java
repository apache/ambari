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

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.MaintenanceState;
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
@EagerSingleton
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
   * Listens for when an alert's state has changed and creates
   * {@link AlertNoticeEntity} instances when appropriate to notify
   * {@link AlertTargetEntity}.
   * <p/>
   * {@link AlertNoticeEntity} are only created when the target has the
   * {@link AlertState} in its list of states.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAlertEvent(AlertStateChangeEvent event) {
    LOG.debug(event);

    // don't create any outbound alert notices if in MM
    AlertCurrentEntity currentAlert = event.getCurrentAlert();
    if (null != currentAlert
        && currentAlert.getMaintenanceState() != MaintenanceState.OFF) {
      return;
    }

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
        if (!isAlertTargetInterested(target, history)) {
          continue;
        }

        AlertNoticeEntity notice = new AlertNoticeEntity();
        notice.setUuid(UUID.randomUUID().toString());
        notice.setAlertTarget(target);
        notice.setAlertHistory(event.getNewHistoricalEntry());
        notice.setNotifyState(NotificationState.PENDING);

        m_alertsDispatchDao.create(notice);
      }
    }
  }

  /**
   * Gets whether the {@link AlertTargetEntity} is interested in receiving a
   * notification about the {@link AlertHistoryEntity}'s state change.
   *
   * @param target
   *          the target (not {@code null}).
   * @param history
   *          the history entry that represents the state change (not
   *          {@code null}).
   * @return {@code true} if the target cares about this state change,
   *         {@code false} otherwise.
   */
  private boolean isAlertTargetInterested(AlertTargetEntity target,
      AlertHistoryEntity history) {
    Set<AlertState> alertStates = target.getAlertStates();
    if (null != alertStates && alertStates.size() > 0) {
      if (!alertStates.contains(history.getAlertState())) {
        return false;
      }
    }

    return true;
  }
}
