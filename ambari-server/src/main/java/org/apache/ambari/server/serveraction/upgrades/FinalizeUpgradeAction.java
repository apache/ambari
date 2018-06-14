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
package org.apache.ambari.server.serveraction.upgrades;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.StackUpgradeFinishEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ModuleComponent;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.Mpack.MpackChangeSummary;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.text.StrBuilder;

import com.google.inject.Inject;

/**
 * Action that represents finalizing the Upgrade by completing any database changes.
 */
public class FinalizeUpgradeAction extends AbstractUpgradeServerAction {

  public static final String PREVIOUS_UPGRADE_NOT_COMPLETED_MSG = "It is possible that a previous upgrade was not finalized. " +
      "For this reason, Ambari will not remove any configs. Please ensure that all database records are correct.";

  @Inject
  private HostDAO hostDAO;

  @Inject
  private HostComponentStateDAO hostComponentStateDAO;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private VersionEventPublisher versionEventPublisher;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);

    UpgradeContext upgradeContext = getUpgradeContext(cluster);

    if (upgradeContext.getDirection() == Direction.UPGRADE) {
      return finalizeUpgrade(upgradeContext);
    } else {
      return finalizeDowngrade(upgradeContext);
    }
  }

  /**
   * Execution path for upgrade.
   * @return the command report
   */
  private CommandReport finalizeUpgrade(UpgradeContext upgradeContext)
    throws AmbariException, InterruptedException {

    Direction direction = upgradeContext.getDirection();

    StringBuilder outSB = new StringBuilder();
    StringBuilder errSB = new StringBuilder();

    try {
      Cluster cluster = upgradeContext.getCluster();
      Map<ServiceGroup, MpackChangeSummary> serviceGroupChanges = upgradeContext.getServiceGroups();
      for (ServiceGroup serviceGroup : serviceGroupChanges.keySet()) {
        MpackChangeSummary mpackChangeSummary = serviceGroupChanges.get(serviceGroup);

        String message = MessageFormat.format(
            "Finalizing the upgrade to {0} for the service group {1}",
            mpackChangeSummary.getTarget().getVersion(), serviceGroup.getServiceGroupName());

        outSB.append(message).append(System.lineSeparator());
      }

      // iterate through all host components and make sure that they are on the
      // correct version; if they are not, then this will throw an exception
      Set<InfoTuple> errors = validateComponentVersions(upgradeContext);
      if (!errors.isEmpty()) {
        StrBuilder messageBuff = new StrBuilder(String.format(
            "The following %d host component(s) have not been upgraded to their expected version:")).append(
                System.lineSeparator());

        errors.stream().forEach(infoTuple -> messageBuff.append(infoTuple));
        throw new AmbariException(messageBuff.toString());
      }

      // do some cleanup like resetting the upgrade state
      for (HostEntity hostEntity : hostDAO.findAll()) {
        Collection<HostComponentStateEntity> hostComponentStates = hostEntity.getHostComponentStateEntities();
        for (HostComponentStateEntity hostComponentStateEntity: hostComponentStates) {
          hostComponentStateEntity.setUpgradeState(UpgradeState.NONE);
          hostComponentStateDAO.merge(hostComponentStateEntity);
        }
      }

      // mark revertable
      boolean revertable = false;
      if (revertable && direction == Direction.UPGRADE) {
        UpgradeEntity upgrade = cluster.getUpgradeInProgress();
        upgrade.setRevertAllowed(true);
        upgrade = m_upgradeDAO.merge(upgrade);
      }

      // Reset upgrade state
      cluster.setUpgradeEntity(null);

      // the upgrade is done!
      versionEventPublisher.publish(new StackUpgradeFinishEvent(cluster));

      StringBuilder finalMessage = new StringBuilder("The upgrade for the following service groups has completed.");
      finalMessage.append(System.lineSeparator());
      finalMessage.append(upgradeContext.getServiceGroupDisplayableSummary());

      outSB.append(finalMessage).append(System.lineSeparator());
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
      Map<ServiceGroup, MpackChangeSummary> serviceGroupChanges = upgradeContext.getServiceGroups();
      for (ServiceGroup serviceGroup : serviceGroupChanges.keySet()) {
        MpackChangeSummary mpackChangeSummary = serviceGroupChanges.get(serviceGroup);

        String message = MessageFormat.format(
            "Finalizing the downgrade from {0} for the service group {1}",
            mpackChangeSummary.getTarget().getVersion(), serviceGroup.getServiceGroupName());

        outSB.append(message).append(System.lineSeparator());
      }

      // iterate through all host components and make sure that they are on the
      // correct version; if they are not, then this will throw an exception
      Set<InfoTuple> errors = validateComponentVersions(upgradeContext);
      if (!errors.isEmpty()) {
        StrBuilder messageBuff = new StrBuilder(String.format(
            "The following %d host component(s) have not been downgraded to their expected version:")).append(
                System.lineSeparator());

        errors.stream().forEach(infoTuple -> messageBuff.append(infoTuple));
        outSB.append(messageBuff.toString()).append(System.lineSeparator());
      }

      // do some cleanup like resetting the upgrade state
      for (HostEntity hostEntity : hostDAO.findAll()) {
        Collection<HostComponentStateEntity> hostComponentStates = hostEntity.getHostComponentStateEntities();
        for (HostComponentStateEntity hostComponentStateEntity : hostComponentStates) {
          hostComponentStateEntity.setUpgradeState(UpgradeState.NONE);
          hostComponentStateDAO.merge(hostComponentStateEntity);
        }
      }

      // remove any configurations for services which crossed a stack boundary
      for (ServiceGroup serviceGroup : serviceGroupChanges.keySet()) {
        MpackChangeSummary mpackChangeSummary = serviceGroupChanges.get(serviceGroup);
        Mpack sourceMpack = mpackChangeSummary.getSource();
        Mpack targetMpack = mpackChangeSummary.getTarget();
        StackId sourceStackId = sourceMpack.getStackId();
        StackId targetStackId = targetMpack.getStackId();

        for (Service service : serviceGroup.getServices()) {
          // only work with configurations when crossing stacks
          if (!sourceStackId.equals(targetStackId)) {
            outSB.append(
                String.format("Removing %s configurations for %s", sourceStackId,
                    service.getName())).append(System.lineSeparator());

            cluster.removeConfigurations(sourceStackId,
                cluster.getService(serviceGroup.getServiceGroupName(),
                    service.getName()).getServiceId());
          }
        }
      }

      // ensure that when downgrading, we set the desired back to the
      // original value
      versionEventPublisher.publish(new StackUpgradeFinishEvent(cluster));

      // Reset upgrade state
      cluster.setUpgradeEntity(null);

      StringBuilder finalMessage = new StringBuilder("The downgrade for the following service groups has completed.");
      finalMessage.append(System.lineSeparator());
      finalMessage.append(upgradeContext.getServiceGroupDisplayableSummary());

      outSB.append(finalMessage).append(System.lineSeparator());

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outSB.toString(), errSB.toString());
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      errSB.append(sw);

      return createCommandReport(-1, HostRoleStatus.FAILED, "{}", outSB.toString(), errSB.toString());
    }
  }

  /**
   * Gets any host components which have not been propertly upgraded or
   * downgraded.
   *
   * @param upgradeContext
   *          the upgrade context (not {@code null}).
   * @return a list of {@link InfoTuple} representing components which should
   *         have been upgraded but did not.
   */
  protected Set<InfoTuple> validateComponentVersions(UpgradeContext upgradeContext)
      throws AmbariException {

    Set<InfoTuple> errors = new TreeSet<>();

    Map<ServiceGroup, MpackChangeSummary> serviceGroupChanges = upgradeContext.getServiceGroups();
    for (ServiceGroup serviceGroup : serviceGroupChanges.keySet()) {
      MpackChangeSummary mpackChangeSummary = serviceGroupChanges.get(serviceGroup);
      Mpack targetMpack = mpackChangeSummary.getTarget();
      StackId targetStackId = targetMpack.getStackId();

      for (Service service : serviceGroup.getServices()) {
        for (ServiceComponent serviceComponent : service.getServiceComponents().values()) {
          for (ServiceComponentHost serviceComponentHost : serviceComponent.getServiceComponentHosts().values()) {
            ComponentInfo componentInfo = ambariMetaInfo.getComponent(targetStackId.getStackName(),
                    targetStackId.getStackVersion(), service.getServiceType(), serviceComponent.getName());

            if (!componentInfo.isVersionAdvertised()) {
              continue;
            }

            ModuleComponent moduleComponent = targetMpack.getModuleComponent(
                serviceComponent.getServiceName(), serviceComponent.getName());

            if (!StringUtils.equals(moduleComponent.getVersion(), serviceComponentHost.getVersion())) {
              InfoTuple error = new InfoTuple(service.getName(), serviceComponent.getName(),
                  serviceComponentHost.getHostName(), serviceComponentHost.getVersion(),
                  moduleComponent.getVersion());

              errors.add(error);
            }
          }
        }
      }
    }
    return errors;
  }

  protected static class InfoTuple implements Comparable<InfoTuple> {
    protected final String serviceName;
    protected final String componentName;
    protected final String hostName;
    protected final String currentVersion;
    protected final String targetVersion;

    protected InfoTuple(String service, String component, String host, String version,
        String desiredVersion) {
      serviceName = service;
      componentName = component;
      hostName = host;
      currentVersion = version;
      targetVersion = desiredVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(InfoTuple that) {
      int compare = hostName.compareTo(that.hostName);
      if (compare != 0) {
        return compare;
      }

      compare = serviceName.compareTo(that.serviceName);
      if (compare != 0) {
        return compare;
      }

      compare = componentName.compareTo(that.componentName);
      if (compare != 0) {
        return compare;
      }

      return compare;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(hostName, serviceName, componentName, currentVersion, targetVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }

      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      InfoTuple that = (InfoTuple) object;

      EqualsBuilder equalsBuilder = new EqualsBuilder();
      equalsBuilder.append(hostName, that.hostName);
      equalsBuilder.append(serviceName, that.serviceName);
      equalsBuilder.append(componentName, that.componentName);
      equalsBuilder.append(currentVersion, that.currentVersion);
      equalsBuilder.append(targetVersion, that.targetVersion);
      ;
      return equalsBuilder.isEquals();
    }

    /**
     * Used for outputting error messages during upgrade.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return String.format("%s on host %s reported %s when %s was expected\n", componentName,
          hostName, currentVersion, targetVersion);
    }
  }
}
