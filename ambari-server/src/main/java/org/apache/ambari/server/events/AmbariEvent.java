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


/**
 * The {@link AmbariEvent} class is the base for all events in Ambari.
 */
public abstract class AmbariEvent {

  /**
   * The {@link AmbariEventType} defines the type of Ambari event.
   */
  public enum AmbariEventType {
    /**
     * A service was successfully installed.
     */
    SERVICE_INSTALL_SUCCESS;
  }

  /**
   * The concrete event's type.
   */
  protected final AmbariEventType m_eventType;

  /**
   * The cluster ID.
   */
  protected final long m_clusterId;

  /**
   * Constructor.
   *
   * @param eventType
   *          the type of event (not {@code null}).
   * @param clusterId
   */
  public AmbariEvent(AmbariEventType eventType, long clusterId) {
    m_eventType = eventType;
    m_clusterId = clusterId;
  }

  /**
   * Gets the cluster ID that the event belongs to.
   *
   * @return the ID of the cluster.
   */
  public long getClusterId() {
    return m_clusterId;
  }

  /**
   * Gets the type of {@link AmbariEvent}.
   *
   * @return the event type (never {@code null}).
   */
  public AmbariEventType getType() {
    return m_eventType;
  }
}
