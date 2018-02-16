/*
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

package org.apache.ambari.server.agent.stomp;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.ambari.server.events.AmbariHostUpdateEvent;
import org.apache.ambari.server.events.publishers.StateUpdateEventPublisher;

/**
 * Is used to saving and updating last version of event in host scope
 * @param <T> event with hash to control version
 */
public abstract class AgentHostDataHolder<T extends AmbariHostUpdateEvent & Hashable> extends AgentDataHolder<T> {

  @Inject
  private StateUpdateEventPublisher stateUpdateEventPublisher;

  private final Map<Long, T> data = new ConcurrentHashMap<>();

  protected abstract T getCurrentData(Long hostId) throws AmbariException;
  protected abstract boolean handleUpdate(T update) throws AmbariException;

  public T getUpdateIfChanged(String agentHash, Long hostId) throws AmbariException {
    T hostData = initializeDataIfNeeded(hostId, true);
    return !Objects.equals(agentHash, hostData.getHash()) ? hostData : getEmptyData();
  }

  public T initializeDataIfNeeded(Long hostId, boolean regenerateHash) throws AmbariException {
    T hostData = data.get(hostId);
    if (hostData == null) {
      hostData = getCurrentData(hostId);
      if (regenerateHash) {
        regenerateDataIdentifiers(hostData);
      }
      data.put(hostId, hostData);
    }
    return hostData;
  }

  /**
   * Apply an incremental update to the data (host-specific), and publish the
   * event to listeners.
   */
  public final void updateData(T update) throws AmbariException {
    initializeDataIfNeeded(update.getHostId(), false);
    if (handleUpdate(update)) {
      T hostData = getData(update.getHostId());
      regenerateDataIdentifiers(hostData);
      setIdentifiersToEventUpdate(update, hostData);
      stateUpdateEventPublisher.publish(update);
    }
  }

  protected void setIdentifiersToEventUpdate(T update, T hostData) {
    update.setHash(hostData.getHash());
  }

  /**
   * Reset data for the given host.  Used if changes are complex and it's easier to re-create data from scratch.
   */
  public final void resetData(Long hostId) throws AmbariException {
    T newData = getCurrentData(hostId);
    data.replace(hostId, newData);
    stateUpdateEventPublisher.publish(newData);
  }

  /**
   * Remove data for the given host.
   */
  public final void onHostRemoved(String hostId) {
    data.remove(hostId);
  }

  public Map<Long, T> getData() {
    return data;
  }

  public T getData(Long hostId) {
    return data.get(hostId);
  }

  public void setData(T data, Long hostId) {
    this.data.put(hostId, data);
  }
}
