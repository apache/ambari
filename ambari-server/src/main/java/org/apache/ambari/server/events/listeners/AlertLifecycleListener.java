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

import org.apache.ambari.server.events.AlertDefinitionRegistrationEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.alert.AggregateDefinitionMapping;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.SourceType;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AlertLifecycleListener} handles events that are part of the alert
 * infrastructure lifecycle such as definition registration events.
 */
@Singleton
public class AlertLifecycleListener {

  /**
   * Used for quick lookups of aggregate alerts.
   */
  @Inject
  private AggregateDefinitionMapping m_aggregateMapping;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertLifecycleListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Handles {@link AlertDefinitionRegistrationEvent} by performing the
   * following tasks:
   * <ul>
   * <li>Registration with {@link AggregateDefinitionMapping}</li>
   * </ul>
   *
   * @param event
   *          the event being handled.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(AlertDefinitionRegistrationEvent event) {
    AlertDefinition definition = event.getDefinition();

    if (definition.getSource().getType() == SourceType.AGGREGATE) {
      m_aggregateMapping.addAggregateType(event.getClusterId(), definition);
    }
  }
}
