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

import java.util.Objects;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.ambari.server.events.STOMPEvent;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;

/**
 * Is used to saving and updating last version of event in cluster scope
 * @param <T> event with hash to control version
 */
public abstract class AgentClusterDataHolder<T extends STOMPEvent & Hashable> extends AgentDataHolder<T> {

  @Inject
  protected STOMPUpdatePublisher STOMPUpdatePublisher;

  private T data;

  public T getUpdateIfChanged(String agentHash) throws AmbariException {
    initializeDataIfNeeded(true);
    return !Objects.equals(agentHash, data.getHash()) ? data : getEmptyData();
  }

  /**
   * Builds an update with the full set of current data.
   * The eventType should be "CREATE", if applicable.
   */
  protected abstract T getCurrentData() throws AmbariException;

  /**
   * Handle an incremental update to the data.
   * @return true if the update introduced any change
   */
  protected abstract boolean handleUpdate(T update) throws AmbariException;

  /**
   * Template method to update the data.
   * @return true if the update introduced any change
   */
  public boolean updateData(T update) throws AmbariException {
    initializeDataIfNeeded(false);
    boolean changed = handleUpdate(update);
    if (changed) {
      regenerateDataIdentifiers(data);
      update.setHash(getData().getHash());
      STOMPUpdatePublisher.publish(update);
    } else {
      // in case update does not have changes empty identifiers should be populated anyway
      if (!isIdentifierValid(data)) {
        regenerateDataIdentifiers(data);
      }
    }
    return changed;
  }

  protected final void initializeDataIfNeeded(boolean regenerateHash) throws AmbariException {
    if (data == null) {
      data = getCurrentData();
      if (regenerateHash) {
        regenerateDataIdentifiers(data);
      }
    }
  }

  public final T getData() {
    return data;
  }

}
