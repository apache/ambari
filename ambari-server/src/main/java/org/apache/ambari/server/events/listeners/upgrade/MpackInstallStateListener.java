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
package org.apache.ambari.server.events.listeners.upgrade;

import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.HostsAddedEvent;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.MpackRegisteredEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.logging.LockFactory;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.dao.MpackHostStateDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.MpackHostStateEntity;
import org.apache.ambari.server.state.MpackInstallState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Striped;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link MpackInstallStateListener} is used to respond to Ambari events
 * which deal with hosts and installations and will create or update the
 * relevent {@link MpackHostStateEntity} instances.
 */
@Singleton
@EagerSingleton
public class MpackInstallStateListener {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(MpackInstallStateListener.class);

  @Inject
  private Provider<HostDAO> m_hostDAO;

  @Inject
  private Provider<MpackHostStateDAO> m_mpackHostStateDAO;

  @Inject
  private Provider<MpackDAO> m_mpackDAOProvider;

  /**
   * Used for ensuring that the concurrent nature of the event handler methods
   * don't collide when attempting to register hosts and mpacks at the same
   * time.
   */
  private Striped<Lock> m_locksByHost = Striped.lazyWeakLock(50);

  /**
   * Constructor.
   *
   * @param ambariEventPublisher
   * @param lockFactory
   */
  @Inject
  public MpackInstallStateListener(AmbariEventPublisher ambariEventPublisher, LockFactory lockFactory) {
    ambariEventPublisher.register(this);
  }

  /**
   * Handles the creation and association of an mpack installation state to the
   * new host. This is done for every registered mpack in the system.
   * <p/>
   * When a host is removed from the system (via a {@link HostsRemovedEvent} no
   * action is needed since the parent {@link HostEntity} will cleanup its
   * orphans.
   */
  @Subscribe
  @Transactional
  public void onHostEvent(HostsAddedEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    HostDAO hostDAO = m_hostDAO.get();
    MpackHostStateDAO mpackHostStateDAO = m_mpackHostStateDAO.get();
    List<MpackEntity> mpackEntities = m_mpackDAOProvider.get().findAll();

    for (String hostName : event.getHostNames()) {
      Lock lock = m_locksByHost.get(hostName);
      lock.lock();
      try {
        // find and remove and mpack install states for the host being added
        List<MpackHostStateEntity> mpackHostStates = mpackHostStateDAO.findByHost(hostName);
        for( MpackHostStateEntity mpackHostState : mpackHostStates ) {
          mpackHostStateDAO.remove(mpackHostState);
        }

        // add a host state for every mpack
        HostEntity hostEntity = hostDAO.findByName(hostName);
        for (MpackEntity mpackEntity : mpackEntities) {
          MpackHostStateEntity mpackHostStateEntity = new MpackHostStateEntity(mpackEntity,
              hostEntity, MpackInstallState.NOT_INSTALLED);

          mpackHostStateDAO.create(mpackHostStateEntity);

          hostEntity.getMpackInstallStates().add(mpackHostStateEntity);
          hostEntity = hostDAO.merge(hostEntity);
        }
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Creates a new installation state for the registered management pack on
   * every host.
   */
  @Subscribe
  @Transactional
  public void onMpackEvent(MpackRegisteredEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    HostDAO hostDAO = m_hostDAO.get();
    MpackHostStateDAO mpackHostStateDAO = m_mpackHostStateDAO.get();

    List<HostEntity> hosts = hostDAO.findAll();
    MpackEntity mpackEntity = m_mpackDAOProvider.get().findById(event.getMpackIdId());

    for (HostEntity hostEntity : hosts) {
      Lock lock = m_locksByHost.get(hostEntity.getHostName());
      lock.lock();
      try {
        // add a host state for mpack
        MpackHostStateEntity mpackHostStateEntity = new MpackHostStateEntity(mpackEntity,
            hostEntity, MpackInstallState.NOT_INSTALLED);

        mpackHostStateDAO.create(mpackHostStateEntity);

        hostEntity.getMpackInstallStates().add(mpackHostStateEntity);
        hostEntity = hostDAO.merge(hostEntity);
        }
      finally {
        lock.unlock();
      }
    }
  }
}
