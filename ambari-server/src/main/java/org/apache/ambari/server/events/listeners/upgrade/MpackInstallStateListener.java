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

import javax.annotation.Nullable;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.events.CommandReportReceivedEvent;
import org.apache.ambari.server.events.HostsAddedEvent;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.MpackRegisteredEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.CommandReportEventPublisher;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.dao.MpackHostStateDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.MpackHostStateEntity;
import org.apache.ambari.server.state.MpackInstallState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Striped;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
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
  private Provider<HostDAO> m_hostDAOProvider;

  @Inject
  private Provider<MpackHostStateDAO> m_mpackHostStateDAOProvider;

  @Inject
  private Provider<MpackDAO> m_mpackDAOProvider;

  /**
   * For deserializing installation command structured output.
   */
  @Inject
  private Gson m_gson;

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
  public MpackInstallStateListener(AmbariEventPublisher ambariEventPublisher,
      CommandReportEventPublisher commandReportEventPublisher) {
    ambariEventPublisher.register(this);
    commandReportEventPublisher.register(this);
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
  public void onHostEvent(HostsAddedEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    HostDAO hostDAO = m_hostDAOProvider.get();
    MpackHostStateDAO mpackHostStateDAO = m_mpackHostStateDAOProvider.get();
    List<MpackEntity> mpackEntities = m_mpackDAOProvider.get().findAll();

    for (String hostName : event.getHostNames()) {
      Lock lock = m_locksByHost.get(hostName);
      lock.lock();
      try {
        HostEntity hostEntity = hostDAO.findByName(hostName);
        hostEntity.getMpackInstallStates().clear();
        hostEntity = hostDAO.merge(hostEntity);

        // add a host state for every mpack
        for (MpackEntity mpackEntity : mpackEntities) {
          MpackHostStateEntity mpackHostStateEntity = new MpackHostStateEntity(mpackEntity,
              hostEntity, MpackInstallState.NOT_INSTALLED);

          mpackHostStateDAO.create(mpackHostStateEntity);

          hostEntity.getMpackInstallStates().add(mpackHostStateEntity);
          hostEntity = hostDAO.merge(hostEntity);
        }
      } catch (Throwable throwable) {
        LOG.error(throwable.getMessage(), throwable);
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

    HostDAO hostDAO = m_hostDAOProvider.get();
    MpackHostStateDAO mpackHostStateDAO = m_mpackHostStateDAOProvider.get();

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
      } catch (Throwable throwable) {
        LOG.error(throwable.getMessage(), throwable);
      }
      finally {
        lock.unlock();
      }
    }
  }

  /**
   * Handles the {@link CommandReportReceivedEvent} which is sent when a
   * command finishes. This will determine if the command was an installation
   * command and then update the appropriate mpack install states.
   *
   * @param event
   */
  @Subscribe
  public void onCommandFinished(CommandReportReceivedEvent event) {
    CommandReport commandReport = event.getCommandReport();
    if (null == commandReport) {
      LOG.error(
          "There is no command report for {} on {}. The management pack installation state cannot be updated.",
          event.getRole(), event.getHostname());
      return;
    }

    String hostName = event.getHostname();
    if (StringUtils.isEmpty(hostName)) {
      LOG.error("The installation command does not contain a host name");
      return;
    }

    // determine if this is an installation command of some type
    String role = event.getRole();
    String roleCommand = commandReport.getRoleCommand();

    if (!StringUtils.equals(role, Role.INSTALL_PACKAGES.name())
        && !StringUtils.equals(roleCommand, RoleCommand.INSTALL.name())) {
      return;
    }

    LOG.debug("Received event {}", event);

    MpackHostStateDAO mpackHostStateDAO = m_mpackHostStateDAOProvider.get();

    // start out with FAILED
    MpackInstallState mpackInstallState = MpackInstallState.INSTALL_FAILED;

    @Nullable
    InstallCommandStructuredOutput structuredOutput = null;

    @Nullable
    Long mpackId = null;

    try {
      // try to parse the structured output of the install command
      structuredOutput = m_gson.fromJson(commandReport.getStructuredOut(),
          InstallCommandStructuredOutput.class);
    } catch (JsonSyntaxException jsonException) {
      LOG.error(
          "Unable to parse the installation structured output for command {} for {} on host {}",
          commandReport.getTaskId(), event.getRole(), hostName, jsonException);
    }

    if (null == structuredOutput || null == structuredOutput.mpackId) {
      LOG.error("The structured output for command {} for {} on {} did not contain an mpack ID",
          commandReport.getTaskId(), event.getRole(), hostName);
    } else {
      mpackId = structuredOutput.mpackId;
    }

    if (!StringUtils.equals(HostRoleStatus.COMPLETED.name(), commandReport.getStatus())) {
      LOG.warn(
          "Command {} for {} did not complete on {}. The management pack installation state will be updated to {}.",
          commandReport.getTaskId(), event.getRole(), hostName, mpackInstallState);
    } else {
      mpackInstallState = MpackInstallState.INSTALLED;
    }

    Lock lock = m_locksByHost.get(hostName);
    lock.lock();
    try {
      // if no mpack ID, then find all INSTALLING and set them to failed
      if (null == mpackId) {
        List<MpackHostStateEntity> mpackHostStateEntities = mpackHostStateDAO.findByHostAndInstallState(
            hostName, MpackInstallState.INSTALLING);

        for (MpackHostStateEntity mpackHostStateEntity : mpackHostStateEntities) {
          mpackHostStateEntity.setState(MpackInstallState.INSTALL_FAILED);
          mpackHostStateDAO.merge(mpackHostStateEntity);
        }
      } else {
        // find the mpack for the host and update the state
        MpackHostStateEntity mpackHostStateEntity = mpackHostStateDAO.findByMpackAndHost(
            mpackId, hostName);

        // an odd case, but we'll plan for it
        if( null == mpackHostStateEntity ) {
          HostDAO hostDAO = m_hostDAOProvider.get();
          MpackDAO mpackDAO = m_mpackDAOProvider.get();

          MpackEntity mpackEntity = mpackDAO.findById(mpackId);
          HostEntity hostEntity = hostDAO.findByName(hostName);

          mpackHostStateEntity = new MpackHostStateEntity(mpackEntity, hostEntity, mpackInstallState);
          mpackHostStateDAO.create(mpackHostStateEntity);

          hostEntity.getMpackInstallStates().add(mpackHostStateEntity);
          hostEntity = hostDAO.merge(hostEntity);
        } else {
          // don't set it if it's already marked as installed
          if (mpackHostStateEntity.getState() != MpackInstallState.INSTALLED) {
            mpackHostStateEntity.setState(mpackInstallState);
            mpackHostStateEntity = mpackHostStateDAO.merge(mpackHostStateEntity);
          }
        }
      }
    } catch (Throwable throwable) {
      LOG.error(throwable.getMessage(), throwable);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Used only to parse the structured output when installing mpacks.
   */
  private static class InstallCommandStructuredOutput {
    /**
     * Either SUCCESS or FAIL
     */
    @SerializedName("package_installation_result")
    private String packageInstallationResult;

    /**
     * The actual version returned, even when a failure during install occurs.
     */
    @SerializedName("mpackName")
    private String mpackName;

    /**
     * The repository id that is returned in structured output.
     */
    @SerializedName("mpackId")
    private Long mpackId = null;
  }
}
