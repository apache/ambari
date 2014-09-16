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

import java.text.MessageFormat;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link AlertServiceStateListener} class handles
 * {@link ServiceInstalledEvent} and ensures that {@link AlertDefinitionEntity}
 * and {@link AlertGroupEntity} instances are correctly populated.
 */
@Singleton
public class AlertServiceStateListener {
  /**
   * Logger.
   */
  private static Log LOG = LogFactory.getLog(AlertServiceStateListener.class);

  /**
   * Services metainfo; injected lazily as a {@link Provider} since JPA is not
   * fully initialized when this singleton is eagerly instantiated. See
   * {@link AmbariServer#main(String[])} and the ordering of
   * {@link ControllerModule} and {@link GuiceJpaInitializer}.
   */
  @Inject
  private Provider<AmbariMetaInfo> m_metaInfoProvider;

  /**
   * Used when a service is installed to read alert definitions from the stack
   * and coerce them into {@link AlertDefinitionEntity}.
   */
  @Inject
  private AlertDefinitionFactory m_alertDefinitionFactory;

  /**
   * Used when a service is installed to insert a default
   * {@link AlertGroupEntity} into the database.
   */
  @Inject
  private AlertDispatchDAO m_alertDispatchDao;

  /**
   * Used when a service is installed to insert {@link AlertDefinitionEntity}
   * into the database.
   */
  @Inject
  private AlertDefinitionDAO m_definitionDao;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertServiceStateListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }


  /**
   * Handles service installed events by populating the database with all known
   * alert definitions for the newly installed service and creates the service's
   * default alert group.
   *
   * @param event
   *          the published event being handled (not {@code null}).
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(ServiceInstalledEvent event) {
    LOG.debug(event);

    long clusterId = event.getClusterId();
    String stackName = event.getStackName();
    String stackVersion = event.getStackVersion();
    String serviceName = event.getServiceName();

    // populate alert definitions for the new service from the database, but
    // don't worry about sending down commands to the agents; the host
    // components are not yet bound to the hosts so we'd have no way of knowing
    // which hosts are invalidated; do that in another impl
    try {
      Set<AlertDefinition> alertDefinitions = m_metaInfoProvider.get().getAlertDefinitions(
          stackName, stackVersion, serviceName);

      for (AlertDefinition definition : alertDefinitions) {
        AlertDefinitionEntity entity = m_alertDefinitionFactory.coerce(
            clusterId,
            definition);

        m_definitionDao.create(entity);
      }
    } catch (AmbariException ae) {
      String message = MessageFormat.format(
          "Unable to populate alert definitions from the database during installation of {0}",
          serviceName);
      LOG.error(message, ae);
    }

    // create the default alert group for the new service
    AlertGroupEntity serviceAlertGroup = new AlertGroupEntity();
    serviceAlertGroup.setClusterId(clusterId);
    serviceAlertGroup.setDefault(true);
    serviceAlertGroup.setGroupName(serviceName);
    serviceAlertGroup.setServiceName(serviceName);

    m_alertDispatchDao.create(serviceAlertGroup);
  }
}
