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
package org.apache.ambari.server.serveraction.upgrades;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.StackUpgradeFinishEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentHistoryEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;

import com.google.inject.Inject;

/**
 * Action that represents finalizing the Upgrade by completing any database changes.
 */
public class FinalizeUpgradeAction extends AbstractUpgradeServerAction {

  public static final String PREVIOUS_UPGRADE_NOT_COMPLETED_MSG = "It is possible that a previous upgrade was not finalized. " +
      "For this reason, Ambari will not remove any configs. Please ensure that all database records are correct.";

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private HostComponentStateDAO hostComponentStateDAO;

  /**
   * Gets {@link StackEntity} instances from {@link StackId}.
   */
  @Inject
  private StackDAO stackDAO;

  /**
   * Gets desired state entities for service components.
   */
  @Inject
  private ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  VersionEventPublisher versionEventPublisher;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = m_clusters.getCluster(clusterName);

    UpgradeContext upgradeContext = getUpgradeContext(cluster);

    if (upgradeContext.getDirection() == Direction.UPGRADE) {
      return finalizeUpgrade(upgradeContext);
    } else {
      return finalizeDowngrade(upgradeContext);
    }
  }

  /**
   * Execution path for upgrade.
   * @param clusterName the name of the cluster the upgrade is for
   * @param version     the target version of the upgrade
   * @return the command report
   */
  private CommandReport finalizeUpgrade(UpgradeContext upgradeContext)
    throws AmbariException, InterruptedException {

    StringBuilder outSB = new StringBuilder();
    StringBuilder errSB = new StringBuilder();

    try {
      String message;
      Set<String> servicesInUpgrade = upgradeContext.getSupportedServices();
      if (servicesInUpgrade.isEmpty()) {
        message = MessageFormat.format("Finalizing the upgrade to {0} for all cluster services.",
            upgradeContext.getVersion());
      } else {
        message = MessageFormat.format(
            "Finalizing the upgrade to {0} for the following services: {1}",
            upgradeContext.getVersion(), StringUtils.join(servicesInUpgrade, ','));
      }

      outSB.append(message).append(System.lineSeparator());

      Cluster cluster = upgradeContext.getCluster();
      String version = upgradeContext.getVersion();
      RepositoryVersionEntity repositoryVersion = upgradeContext.getTargetRepositoryVersion();

      // iterate through all host components and make sure that they are on the
      // correct version; if they are not, then this will throw an exception
      List<InfoTuple> errors = getHostComponentsWhichDidNotUpgrade(upgradeContext);
      if (!errors.isEmpty()) {
        StrBuilder messageBuff = new StrBuilder(String.format(
            "The following %d host component(s) "
                + "have not been upgraded to version %s. Please install and upgrade "
                + "the Stack Version on those hosts and try again.\nHost components:",
            errors.size(), version)).append(System.lineSeparator());

        for (InfoTuple error : errors) {
          messageBuff.append(String.format("%s on host %s\n", error.componentName, error.hostName));
        }

        throw new AmbariException(messageBuff.toString());
      }

      // for all hosts participating in this upgrade, update thei repository
      // versions and upgrade state
      List<HostVersionEntity> hostVersions = hostVersionDAO.findHostVersionByClusterAndRepository(
          cluster.getClusterId(), repositoryVersion);

      Set<HostVersionEntity> hostVersionsAllowed = new HashSet<>();
      Set<String> hostsWithoutCorrectVersionState = new HashSet<>();

      // for every host version for this repository, determine if any didn't
      // transition correctly
      for (HostVersionEntity hostVersion : hostVersions) {
        RepositoryVersionState hostVersionState = hostVersion.getState();
        switch( hostVersionState ){
          case CURRENT:
          case NOT_REQUIRED: {
            hostVersionsAllowed.add(hostVersion);
            break;
          }
          default: {
            hostsWithoutCorrectVersionState.add(hostVersion.getHostName());
            break;
          }
        }
      }

      // throw an exception if there are hosts which are not not fully upgraded
      if (hostsWithoutCorrectVersionState.size() > 0) {
        message = String.format("The following %d host(s) have not been upgraded to version %s. " +
                "Please install and upgrade the Stack Version on those hosts and try again.\nHosts: %s",
            hostsWithoutCorrectVersionState.size(),
            version,
            StringUtils.join(hostsWithoutCorrectVersionState, ", "));
        outSB.append(message);
        outSB.append(System.lineSeparator());
        throw new AmbariException(message);
      }

      outSB.append(
          String.format("Finalizing the upgrade state of %d host(s).",
              hostVersionsAllowed.size())).append(System.lineSeparator());

      // Reset the upgrade state
      for (HostVersionEntity hostVersion : hostVersionsAllowed) {
        Collection<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(hostVersion.getHostName());
        for (HostComponentStateEntity hostComponentStateEntity: hostComponentStates) {
          hostComponentStateEntity.setUpgradeState(UpgradeState.NONE);
          hostComponentStateDAO.merge(hostComponentStateEntity);
        }
      }

      // Impacts all hosts that have a version
      outSB.append(
          String.format("Finalizing the version for %d host(s).",
              hostVersionsAllowed.size())).append(System.lineSeparator());

      versionEventPublisher.publish(new StackUpgradeFinishEvent(cluster));

      outSB.append("Creating upgrade history...").append(System.lineSeparator());
      writeComponentHistory(upgradeContext);

      // Reset upgrade state
      cluster.setUpgradeEntity(null);

      message = String.format("The upgrade to %s has completed.", upgradeContext.getVersion());
      outSB.append(message).append(System.lineSeparator());
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outSB.toString(), errSB.toString());
    } catch (Exception e) {
      errSB.append(e.getMessage());
      return createCommandReport(-1, HostRoleStatus.FAILED, "{}", outSB.toString(), errSB.toString());
    }
  }

  /**
   * Execution path for downgrade.
   *
   * @param upgradeContext
   *          the upgrade context (not {@code null}).
   * @return the command report
   */
  private CommandReport finalizeDowngrade(UpgradeContext upgradeContext)
      throws AmbariException, InterruptedException {

    StringBuilder outSB = new StringBuilder();
    StringBuilder errSB = new StringBuilder();

    try {
      Cluster cluster = upgradeContext.getCluster();
      RepositoryVersionEntity repositoryVersion = upgradeContext.getTargetRepositoryVersion();

      String message;
      Set<String> servicesInUpgrade = upgradeContext.getSupportedServices();
      if (servicesInUpgrade.isEmpty()) {
        message = MessageFormat.format("Finalizing the downgrade to {0} for all cluster services.",
            upgradeContext.getVersion());
      } else {
        message = MessageFormat.format(
            "Finalizing the downgrade to {0} for the following services: {1}",
            upgradeContext.getVersion(), StringUtils.join(servicesInUpgrade, ','));
      }

      outSB.append(message).append(System.lineSeparator());
      outSB.append(message).append(System.lineSeparator());

      // iterate through all host components and make sure that they are on the
      // correct version; if they are not, then this will throw an exception
      List<InfoTuple> errors = getHostComponentsWhichDidNotUpgrade(upgradeContext);
      if (!errors.isEmpty()) {
        StrBuilder messageBuff = new StrBuilder(String.format(
            "The following %d host component(s) " + "have not been downgraded to version %s\n",
            errors.size(), upgradeContext.getVersion())).append(System.lineSeparator());

        for (InfoTuple error : errors) {
          messageBuff.append(String.format("%s on host %s", error.componentName, error.hostName));
          messageBuff.append(System.lineSeparator());
        }

        throw new AmbariException(messageBuff.toString());
      }

      // find host versions
      List<HostVersionEntity> hostVersions = hostVersionDAO.findHostVersionByClusterAndRepository(
          cluster.getClusterId(), repositoryVersion);

      outSB.append(
          String.format("Finalizing the downgrade state of %d host(s).",
              hostVersions.size())).append(
              System.lineSeparator());

      for( HostVersionEntity hostVersion : hostVersions ){
        if (hostVersion.getState() != RepositoryVersionState.CURRENT) {
          hostVersion.setState(RepositoryVersionState.CURRENT);
          hostVersionDAO.merge(hostVersion);
        }

        List<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(
            hostVersion.getHostName());

        for (HostComponentStateEntity hostComponentState : hostComponentStates) {
          hostComponentState.setUpgradeState(UpgradeState.NONE);
          hostComponentStateDAO.merge(hostComponentState);
        }
      }

      // ensure that when downgrading, we set the desired back to the
      // original value
      versionEventPublisher.publish(new StackUpgradeFinishEvent(cluster));

      // Reset upgrade state
      cluster.setUpgradeEntity(null);

      message = String.format("The downgrade to %s has completed.", upgradeContext.getVersion());
      outSB.append(message).append(System.lineSeparator());

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outSB.toString(), errSB.toString());
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      errSB.append(sw.toString());

      return createCommandReport(-1, HostRoleStatus.FAILED, "{}", outSB.toString(), errSB.toString());
    }
  }


  /**
   * Gets any host components which have not been propertly upgraded.
   *
   * @param upgradeContext
   *          the upgrade context (not {@code null}).
   * @return a list of {@link InfoTuple} representing components which should
   *         have been upgraded but did not.
   */
  protected List<InfoTuple> getHostComponentsWhichDidNotUpgrade(UpgradeContext upgradeContext)
          throws AmbariException {

    ArrayList<InfoTuple> errors = new ArrayList<>();

    Cluster cluster = upgradeContext.getCluster();
    Set<String> supportedServices = upgradeContext.getSupportedServices();
    RepositoryVersionEntity repositoryVersionEntity = upgradeContext.getTargetRepositoryVersion();
    StackId targetStackId = repositoryVersionEntity.getStackId();

    for (Service service : cluster.getServices().values()) {

      // !!! if there are supported services for upgrade, and the cluster service is NOT in the list, skip
      if (!supportedServices.isEmpty() && !supportedServices.contains(service.getName())) {
        continue;
      }

      for (ServiceComponent serviceComponent : service.getServiceComponents().values()) {
        for (ServiceComponentHost serviceComponentHost : serviceComponent.getServiceComponentHosts().values()) {
          ComponentInfo componentInfo = ambariMetaInfo.getComponent(targetStackId.getStackName(),
                  targetStackId.getStackVersion(), service.getName(), serviceComponent.getName());

          if (componentInfo.isVersionAdvertised()) {
            if (!StringUtils.equals(upgradeContext.getVersion(),
                serviceComponentHost.getVersion())) {
              errors.add(new InfoTuple(service.getName(), serviceComponent.getName(),
                  serviceComponentHost.getHostName(), serviceComponentHost.getVersion()));
            }
          }
        }
      }
    }

    return errors;
  }

  /**
   * Writes the upgrade history for all components which participated in the
   * upgrade.
   *
   * @param upgradeContext  the upgrade context (not {@code null}).
   */
  private void writeComponentHistory(UpgradeContext upgradeContext) throws AmbariException {
    Cluster cluster = upgradeContext.getCluster();
    UpgradeEntity upgradeEntity = cluster.getUpgradeInProgress();
    Collection<Service> services = cluster.getServices().values();
    RepositoryVersionEntity repositoryVersion = upgradeContext.getTargetRepositoryVersion();
    StackId sourcceStackId = upgradeContext.getOriginalStackId();
    StackId targetStackId = repositoryVersion.getStackId();

    StackEntity fromStack = stackDAO.find(sourcceStackId.getStackName(), sourcceStackId.getStackVersion());
    StackEntity toStack = stackDAO.find(targetStackId.getStackName(), targetStackId.getStackVersion());


    if (!upgradeContext.getSupportedServices().isEmpty()) {
      services = new ArrayList<>();

      Set<String> serviceNames = upgradeContext.getSupportedServices();
      for (String serviceName : serviceNames) {
        services.add(cluster.getService(serviceName));
      }
    }

    // for every service component, if it was included in the upgrade then
    // create a historical entry
    for (Service service : services) {
      for (ServiceComponent serviceComponent : service.getServiceComponents().values()) {
        if (serviceComponent.isVersionAdvertised()) {
          // create the historical entry
          ServiceComponentHistoryEntity historyEntity = new ServiceComponentHistoryEntity();
          historyEntity.setUpgrade(upgradeEntity);
          historyEntity.setFromStack(fromStack);
          historyEntity.setToStack(toStack);

          // get the service component
          ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findByName(
              cluster.getClusterId(), serviceComponent.getServiceName(),
              serviceComponent.getName());

          // add the history to the component and save
          desiredStateEntity.addHistory(historyEntity);
          serviceComponentDesiredStateDAO.merge(desiredStateEntity);
        }
      }
    }
  }

  protected static class InfoTuple {
    protected final String serviceName;
    protected final String componentName;
    protected final String hostName;
    protected final String currentVersion;

    protected InfoTuple(String service, String component, String host, String version) {
      serviceName = service;
      componentName = component;
      hostName = host;
      currentVersion = version;
    }
  }

}
