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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.events.RequestUpdateEvent;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.topology.TopologyManager;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RequestUpdateEventPublisher {

  private final Long TIMEOUT = 1000L;
  private ConcurrentHashMap<Long, Long> previousTime = new ConcurrentHashMap<>();
  private ConcurrentHashMap<Long, RequestUpdateEvent> buffer = new ConcurrentHashMap<>();

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private TopologyManager topologyManager;

  @Inject
  private RequestDAO requestDAO;

  @Inject
  private ClusterDAO clusterDAO;

  public void publish(RequestUpdateEvent event, EventBus m_eventBus) {
    Long eventTime = System.currentTimeMillis();
    Long requestId = event.getRequestId();
    if (!previousTime.containsKey(requestId)) {
      previousTime.put(requestId, 0L);
    }
    if (eventTime - previousTime.get(requestId) <= TIMEOUT && !buffer.containsKey(requestId)) {
      buffer.put(event.getRequestId(), event);
      Executors.newScheduledThreadPool(1).schedule(new RequestEventRunnable(requestId, m_eventBus),
          TIMEOUT, TimeUnit.MILLISECONDS);
    } else if (buffer.containsKey(requestId)) {
      //merge available buffer content with arrived
      buffer.get(requestId).setEndTime(event.getEndTime());
      buffer.get(requestId).setRequestStatus(event.getRequestStatus());
      buffer.get(requestId).setRequestContext(event.getRequestContext());
      buffer.get(requestId).getHostRoleCommands().removeAll(event.getHostRoleCommands());
      buffer.get(requestId).getHostRoleCommands().addAll(event.getHostRoleCommands());
    } else {
      previousTime.put(requestId, eventTime);
      //TODO add logging and metrics posting
      m_eventBus.post(fillRequest(event));
    }
  }

  private RequestUpdateEvent fillRequest(RequestUpdateEvent event) {
    event.setProgressPercent(
        CalculatedStatus.statusFromRequest(hostRoleCommandDAO, topologyManager, event.getRequestId()).getPercent());
    if (event.getEndTime() == null || event.getStartTime() == null || event.getClusterName() == null
        || event.getRequestContext() == null) {
      RequestEntity requestEntity = requestDAO.findByPK(event.getRequestId());
      event.setStartTime(requestEntity.getStartTime());
      event.setUserName(requestEntity.getUserName());
      event.setEndTime(requestEntity.getEndTime());
      if (requestEntity.getClusterId() != -1) {
        event.setClusterName(clusterDAO.findById(requestEntity.getClusterId()).getClusterName());
      }
      event.setRequestContext(requestEntity.getRequestContext());
      event.setRequestStatus(requestEntity.getStatus());
    }
    return event;
  }

  private class RequestEventRunnable implements Runnable {

    private final long requestId;
    private final EventBus eventBus;

    public RequestEventRunnable(long requestId, EventBus eventBus) {
      this.requestId = requestId;
      this.eventBus = eventBus;
    }

    @Override
    public void run() {
      RequestUpdateEvent resultEvent = buffer.get(requestId);
      //TODO add logging and metrics posting
      eventBus.post(fillRequest(resultEvent));
      buffer.remove(requestId);
      previousTime.remove(requestId);
    }
  }
}
