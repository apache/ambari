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
package org.apache.ambari.server.events;

import com.google.common.base.Objects;

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
    SERVICE_INSTALL_SUCCESS,

    /**
     * A service was successfully removed.
     */
    SERVICE_REMOVED_SUCCESS,

    /**
     * A service group was successfully installed.
     */
    SERVICE_GROUP_INSTALL_SUCCESS,

    /**
     * A service group was successfully removed.
     */
    SERVICE_GROUP_REMOVED_SUCCESS,

    /**
     * A service component was successfully installed.
     */
    SERVICE_COMPONENT_INSTALL_SUCCESS,

    /**
     * A service component was successfully uninstalled.
     */
    SERVICE_COMPONENT_UNINSTALLED_SUCCESS,

    /**
     * An alert definition is registered with the system.
     */
    ALERT_DEFINITION_REGISTRATION,

    /**
     * An alert definition was updated.
     */
    ALERT_DEFINITION_CHANGED,

    /**
     * An alert definition is removed from the system.
     */
    ALERT_DEFINITION_REMOVAL,

    /**
     * The alert definition has was invalidated.
     */
    ALERT_DEFINITION_HASH_INVALIDATION,

    /**
     * The alert definition was disabled.
     */
    ALERT_DEFINITION_DISABLED,

    /**
     * A host was registered with the server.
     */
    HOST_REGISTERED,

    /**
     * A host was registered with the server.
     */
    HOST_COMPONENT_VERSION_ADVERTISED,

    /**
     * A host was added to the cluster.
     */
    HOST_ADDED,

    /**
     * A host was removed from the cluster.
     */
    HOST_REMOVED,

    /**
     * A host/service/component has had a maintenance mode change.
     */
    MAINTENANCE_MODE,

    /**
     * Sent when request finishes
     */
    REQUEST_FINISHED,

    /**
     * The cluster was renamed.
     */
    CLUSTER_RENAME,

    /**
     * The service component recovery enabled field changed.
     */
    SERVICE_COMPONENT_RECOVERY_CHANGED,

    /**
     * Stack upgrade or downgrade finishes
     */
    FINALIZE_UPGRADE_FINISH,

    /**
     * Cluster configuration changed.
     */
    CLUSTER_CONFIG_CHANGED,

    /**
     * Cluster configuration finished.
     */
    CLUSTER_CONFIG_FINISHED,

    /**
     * Metrics Collector force refresh needed.
     */
    METRICS_COLLECTOR_HOST_DOWN,

    /**
     * Local user has been created.
     */
    USER_CREATED,

    /**
     * Ambari configuration changed event;
     */
    AMBARI_CONFIGURATION_CHANGED,

    /**
     * JPA initialized
     */
    JPA_INITIALIZED,

    /**
     * A management pack was registered with the system.
     */
    MPACK_REGISTERED,

    /**
     * A management pack was removed from the system.
     */
    MPACK_REMOVED

  }

  /**
   * The concrete event's type.
   */
  protected final AmbariEventType m_eventType;

  /**
   * Constructor.
   *
   * @param eventType the type of event (not {@code null}).
   */
  public AmbariEvent(AmbariEventType eventType) {
    m_eventType = eventType;
  }

  /**
   * Gets the type of {@link AmbariEvent}.
   *
   * @return the event type (never {@code null}).
   */
  public AmbariEventType getType() {
    return m_eventType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("eventType", m_eventType).toString();
  }
}
