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
package org.apache.ambari.server.state.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.notifications.DispatchFactory;
import org.apache.ambari.server.notifications.DispatchRunnable;
import org.apache.ambari.server.notifications.DispatchCallback;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.NotificationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AlertNoticeDispatchService} is used to scan the database for
 * {@link AlertNoticeEntity} that are in the {@link NotificationState#PENDING}.
 * It will then process them through the dispatch system.
 */
@Singleton
public class AlertNoticeDispatchService extends AbstractScheduledService {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertNoticeDispatchService.class);

  /**
   * Dispatch DAO to query pending {@link AlertNoticeEntity} instances from.
   */
  @Inject
  private AlertDispatchDAO m_dao;

  /**
   * The factory used to get an {@link NotificationDispatcher} instance to submit to the
   * {@link #m_executor}.
   */
  @Inject
  private DispatchFactory m_dispatchFactory;

  /**
   * The executor responsible for dispatching.
   */
  private final ThreadPoolExecutor m_executor;

  /**
   * Constructor.
   */
  public AlertNoticeDispatchService() {
    m_executor = new ThreadPoolExecutor(0, 2, 5L,
        TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
        new AlertDispatchThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void runOneIteration() throws Exception {
    List<AlertNoticeEntity> pending = m_dao.findPendingNotices();
    if (pending.size() == 0) {
      return;
    }

    LOG.info("There are {} pending alert notices about to be dispatched..."
        + pending.size());

    // combine all histories by target
    Map<AlertTargetEntity, List<AlertNoticeEntity>> aggregateMap = new HashMap<AlertTargetEntity, List<AlertNoticeEntity>>(
        pending.size());

    for (AlertNoticeEntity notice : pending) {
      AlertTargetEntity target = notice.getAlertTarget();

      List<AlertNoticeEntity> notices = aggregateMap.get(target);
      if (null == notices) {
        notices = new ArrayList<AlertNoticeEntity>();
        aggregateMap.put(target, notices);
      }

      notices.add(notice);
    }

    Set<AlertTargetEntity> targets = aggregateMap.keySet();
    for (AlertTargetEntity target : targets) {
      List<AlertNoticeEntity> notices = aggregateMap.get(target);
      if (null == notices || notices.size() == 0) {
        continue;
      }

      Notification notification = new Notification();
      notification.Subject = target.getTargetName();
      notification.Body = target.getDescription();
      notification.Callback = new AlertNoticeDispatchCallback();
      notification.CallbackIds = new ArrayList<String>(notices.size());

      for (AlertNoticeEntity notice : notices) {
        notification.CallbackIds.add(notice.getUuid());
      }

      NotificationDispatcher dispatcher = m_dispatchFactory.getDispatcher(target.getNotificationType());
      DispatchRunnable runnable = new DispatchRunnable(dispatcher, notification);

      m_executor.execute(runnable);
    }
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Returns a schedule that starts after 1 minute and runs every minute after
   * {@link #runOneIteration()} completes.
   */
  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
  }

  /**
   * A custom {@link ThreadFactory} for the threads that will handle dispatching
   * {@link AlertNoticeEntity} instances. Threads created will have slightly
   * reduced priority since {@link AlertEvent} instances are not critical to the
   * system.
   */
  private static final class AlertDispatchThreadFactory implements
      ThreadFactory {

    private static final AtomicInteger s_threadIdPool = new AtomicInteger(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, "alert-dispatch-"
          + s_threadIdPool.getAndIncrement());

      thread.setDaemon(false);
      thread.setPriority(Thread.NORM_PRIORITY - 1);

      return thread;
    }
  }

  /**
   * The {@link AlertNoticeDispatchCallback} is used to receive a callback from
   * the dispatch framework and then update the {@link AlertNoticeEntity}
   * {@link NotificationState}.
   */
  private final class AlertNoticeDispatchCallback implements
      DispatchCallback {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSuccess(List<String> callbackIds) {
      for (String callbackId : callbackIds) {
        updateAlertNotice(callbackId, NotificationState.DELIVERED);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFailure(List<String> callbackIds) {
      for (String callbackId : callbackIds) {
        updateAlertNotice(callbackId, NotificationState.FAILED);
      }
    }

    /**
     * Updates the {@link AlertNoticeEntity} matching the given UUID with the
     * specified state.
     *
     * @param uuid
     * @param state
     */
    private void updateAlertNotice(String uuid, NotificationState state) {
      try {
        AlertNoticeEntity entity = m_dao.findNoticeByUuid(uuid);
        if (null == entity) {
          LOG.warn("Unable to find an alert notice with UUID {}", uuid);
          return;
        }

        entity.setNotifyState(state);
        m_dao.merge(entity);
      } catch (Exception exception) {
        LOG.error(
            "Unable to update the alert notice with UUID {} to {}, notifications will continue to be sent",
            uuid, state, exception);
      }
    }
  }
}
