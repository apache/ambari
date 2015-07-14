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
package org.apache.ambari.server.events.listeners.upgrade;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.HostComponentVersionEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link StackVersionListener} class handles the propagation of versions
 * advertised by the {@link org.apache.ambari.server.state.ServiceComponentHost}
 * that bubble up to the
 * {@link org.apache.ambari.server.orm.entities.HostVersionEntity} and
 * eventually the
 * {@link org.apache.ambari.server.orm.entities.ClusterVersionEntity}
 */
@Singleton
@EagerSingleton
public class StackVersionListener {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackVersionListener.class);

  /**
   * Used to prevent multiple threads from trying to create host alerts
   * simultaneously.
   */
  private Lock m_stackVersionLock = new ReentrantLock();

  /**
   * Constructor.
   *
   * @param eventPublisher  the publisher
   */
  @Inject
  public StackVersionListener(VersionEventPublisher eventPublisher) {
    eventPublisher.register(this);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onAmbariEvent(HostComponentVersionEvent event) {
    LOG.debug("Received event {}", event);

    Cluster cluster = event.getCluster();

    ServiceComponentHost sch = event.getServiceComponentHost();

    m_stackVersionLock.lock();

    try {
      RepositoryVersionEntity repoVersion = sch.recalculateHostVersionState();
      if (null != repoVersion) {
        cluster.recalculateClusterVersionState(repoVersion);
      }
    } catch (Exception e) {
      LOG.error(
          "Unable to propagate version for ServiceHostComponent on component: {}, host: {}. Error: {}",
          sch.getServiceComponentName(), sch.getHostName(), e.getMessage());
    } finally {
      m_stackVersionLock.unlock();
    }
  }
}
