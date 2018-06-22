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
package org.apache.ambari.server.checks;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.UpgradePlanDetailEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.Mpack.ModuleVersionChange;
import org.apache.ambari.server.state.Mpack.MpackChangeSummary;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * A {@link ClusterCheck} is used to run a pre-upgrade check against a condition
 * that is not dependent on any installed service.
 */
public abstract class ClusterCheck implements PreUpgradeCheck {

  protected static final String DEFAULT = "default";

  @Inject
  protected Provider<Clusters> clustersProvider;

  @Inject
  Provider<UpgradeDAO> upgradeDaoProvider;

  @Inject
  Provider<RepositoryVersionHelper> repositoryVersionHelper;

  @Inject
  Provider<AmbariMetaInfo> ambariMetaInfo;

  @Inject
  Configuration config;

  @Inject
  Gson gson;

  private final CheckDescription m_description;

  /**
   * Constructor.
   *
   * @param description description
   */
  protected ClusterCheck(CheckDescription description) {
    m_description = description;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CheckDescription getCheckDescrption() {
    return m_description;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getApplicableServices() {
    return Collections.emptySet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<CheckQualification> getQualifications() {
    return Collections.emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    List<CheckQualification> qualifications = Lists.newArrayList(
        new ServiceQualification());

    // add any others from the concrete check
    qualifications.addAll(getQualifications());
    for (CheckQualification qualification : qualifications) {
      if (!qualification.isApplicable(request)) {
        return false;
      }
    }

    return true;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public abstract UpgradeCheckResult perform(PrereqCheckRequest request) throws AmbariException;


  /**
   * {@inheritDoc}
   */
  @Override
  public PrereqCheckType getType() {
    return m_description.getType();
  }

  /**
   * Gets the default fail reason
   * @param prerequisiteCheck the check being performed
   * @param request           the request
   * @return the failure string
   */
  protected String getFailReason(UpgradeCheckResult prerequisiteCheck, PrereqCheckRequest request)
      throws AmbariException {
    return getFailReason(DEFAULT, prerequisiteCheck, request);
  }

  /**
   * Gets a cluster configuration property if it exists, or {@code null}
   * otherwise.
   *
   * @param request
   *          the request (not {@code null}).
   * @param configType
   *          the configuration type, such as {@code hdfs-site} (not
   *          {@code null}).
   * @param propertyName
   *          the name of the property (not {@code null}).
   * @return the property value or {@code null} if not found.
   * @throws AmbariException
   */
  protected String getProperty(PrereqCheckRequest request, String configType, String propertyName)
      throws AmbariException {
    UpgradePlanEntity upgradePlan = request.getUpgradePlan();
    final Cluster cluster = clustersProvider.get().getCluster(upgradePlan.getClusterId());
    final Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    final DesiredConfig desiredConfig = desiredConfigs.get(configType);

    if (null == desiredConfig) {
      return null;
    }

    final Config config = cluster.getConfig(configType, desiredConfig.getTag());

    Map<String, String> properties = config.getProperties();
    return properties.get(propertyName);
  }

  /**
   * Gets the fail reason
   * @param key               the failure text key
   * @param prerequisiteCheck the check being performed
   * @param request           the request
   * @return the failure string
   */
  protected String getFailReason(String key, UpgradeCheckResult prerequisiteCheck,
      PrereqCheckRequest request) throws AmbariException {
    UpgradePlanEntity upgradePlan = request.getUpgradePlan();
    String fail = m_description.getFail(key);

    if (fail.contains("{{fails}}")) {
      LinkedHashSet<String> names = prerequisiteCheck.getFailedOn();

      // If Type=PrereqCheckType.HOST, names list is already populated
      if (m_description.getType() == PrereqCheckType.SERVICE) {
        Clusters clusters = clustersProvider.get();
        Cluster c = clusters.getCluster(upgradePlan.getClusterId());
        Map<String, Service> services = c.getServicesByName();

        LinkedHashSet<String> displays = new LinkedHashSet<>();
        for (String name : names) {
          if (services.containsKey(name)) {
            displays.add(services.get(name).getName());
          } else {
            displays.add(name);
          }
        }
        names = displays;

      }

      fail = fail.replace("{{fails}}", formatEntityList(names));
    }

    return fail;
  }

  /**
   * Formats lists of given entities to human readable form:
   * [entity1] -> {entity1} {noun}
   * [entity1, entity2] -> {entity1} and {entity2} {noun}s
   * [entity1, entity2, entity3] -> {entity1}, {entity2} and {entity3} {noun}s
   * The noun for the entities is taken from check type, it may be cluster, service or host.
   *
   * @param entities list of entities to format
   * @return formatted entity list
   */
  protected String formatEntityList(LinkedHashSet<String> entities) {
    if (entities == null || entities.isEmpty()) {
      return "";
    }

    final StringBuilder formatted = new StringBuilder(StringUtils.join(entities, ", "));
    if (entities.size() > 1) {
      formatted.replace(formatted.lastIndexOf(","), formatted.lastIndexOf(",") + 1, " and");
    }

    return formatted.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRequired(UpgradeType upgradeType) {
    UpgradeType[] upgradeTypes = getClass().getAnnotation(UpgradeCheck.class).required();
    for (UpgradeType requiredType : upgradeTypes) {
      if (upgradeType == requiredType) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets the services participating in the upgrade as a flat set of services
   * which are not associated with any service groups.
   *
   * @param request
   *          the upgrade check request.
   * @return the services participating in the upgrade
   */
  final Set<String> getDistinctServicesInUpgrade(PrereqCheckRequest request)
      throws AmbariException {
    Set<String> servicesInUpgrade = Sets.newHashSet();
    UpgradePlanEntity upgradePlan = request.getUpgradePlan();
    final Cluster cluster = clustersProvider.get().getCluster(upgradePlan.getClusterId());
    List<UpgradePlanDetailEntity> details = upgradePlan.getDetails();
    for (UpgradePlanDetailEntity detail : details) {
      ServiceGroup serviceGroup = cluster.getServiceGroup(detail.getServiceGroupId());
      Mpack sourceMpack = ambariMetaInfo.get().getMpack(serviceGroup.getMpackId());
      Mpack targetMpack = ambariMetaInfo.get().getMpack(detail.getMpackTargetId());
      MpackChangeSummary changeSummary = sourceMpack.getChangeSummary(targetMpack);
      Set<ModuleVersionChange> moduleChanges = changeSummary.getModuleVersionChanges();

      servicesInUpgrade.addAll(
          moduleChanges.stream().map(moduleChange -> moduleChange.getSource().getName()).collect(
              Collectors.toSet()));
    }

    return servicesInUpgrade;
  }

  /**
   * Gets the services participating in the upgrade organized by their service
   * groups.
   *
   * @param request
   * @return the service groups and their services which are participating in
   *         the upgrade.
   * @throws AmbariException
   */
  final Map<ServiceGroup, Set<String>> getServicesInUpgrade(PrereqCheckRequest request) throws AmbariException {
    Map<ServiceGroup, Set<String>> serviceGroupsInUpgrade = Maps.newLinkedHashMap();
    UpgradePlanEntity upgradePlan = request.getUpgradePlan();
    final Cluster cluster = clustersProvider.get().getCluster(upgradePlan.getClusterId());
    List<UpgradePlanDetailEntity> details = upgradePlan.getDetails();
    for (UpgradePlanDetailEntity detail : details) {
      Set<String> servicesInUpgrade = Sets.newHashSet();
      ServiceGroup serviceGroup = cluster.getServiceGroup(detail.getServiceGroupId());
      serviceGroupsInUpgrade.put(serviceGroup, servicesInUpgrade);

      Mpack sourceMpack = ambariMetaInfo.get().getMpack(serviceGroup.getMpackId());
      Mpack targetMpack = ambariMetaInfo.get().getMpack(detail.getMpackTargetId());
      MpackChangeSummary changeSummary = sourceMpack.getChangeSummary(targetMpack);
      Set<ModuleVersionChange> moduleChanges = changeSummary.getModuleVersionChanges();

      servicesInUpgrade.addAll(
          moduleChanges.stream().map(moduleChange -> moduleChange.getSource().getName()).collect(
              Collectors.toSet()));
    }

    return serviceGroupsInUpgrade;
  }

  /**
   * The {@link CheckQualification} interface is used to provide multiple
   * different qualifications against which an upgrade check is determined to be
   * applicable to the upgrade.
   */
  interface CheckQualification {

    /**
     * Gets whether the upgrade check meets this qualification and should
     * therefore be run before the upgrade.
     *
     * @param request
     * @return
     * @throws AmbariException
     */
    boolean isApplicable(PrereqCheckRequest request) throws AmbariException;
  }

  /**
   * The {@link ServiceQualification} class is used to determine if the
   * service(s) associated with an upgraade check are both installed in the
   * cluster and included in the upgrade.
   * <p/>
   * If a service is installed but not included in the upgrade (for example of
   * the upgrade is a patch upgrade), then the check should not qualify to run.
   */
  final class ServiceQualification implements CheckQualification {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {

      Set<String> applicableServices = getApplicableServices();

      // if the check is not scoped to any particular service, then it passes
      // this qualification
      if (applicableServices.isEmpty()) {
        return true;
      }

      Set<String> servicesForUpgrade = getDistinctServicesInUpgrade(request);

      for (String serviceInUpgrade : servicesForUpgrade) {
        if (applicableServices.contains(serviceInUpgrade)) {
          return true;
        }
      }

      return false;
    }
  }

  /**
   * The {@link PriorCheckQualification} class is used to determine if a prior check has run.
   */
  final class PriorCheckQualification implements CheckQualification {

    private final CheckDescription m_checkDescription;

    public PriorCheckQualification(CheckDescription checkDescription) {
      m_checkDescription = checkDescription;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
      PrereqCheckStatus checkStatus = request.getResult(m_checkDescription);
      if (null != checkStatus && checkStatus == PrereqCheckStatus.FAIL) {
        return false;
      }

      return true;
    }
  }

  /**
   * Used to represent information about a service. This class is safe to use in
   * sorted & unique collections.
   */
  static class ServiceDetail implements Comparable<ServiceDetail> {
    @JsonProperty("service_name")
    final String serviceName;

    ServiceDetail(String serviceName) {
      this.serviceName = serviceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      ServiceDetail other = (ServiceDetail) obj;
      return Objects.equals(serviceName, other.serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(ServiceDetail other) {
      return serviceName.compareTo(other.serviceName);
    }
  }

  /**
   * Used to represent information about a service component. This class is safe
   * to use in sorted & unique collections.
   */
  static class ServiceComponentDetail implements Comparable<ServiceComponentDetail> {
    @JsonProperty("service_name")
    final String serviceName;

    @JsonProperty("component_name")
    final String componentName;

    ServiceComponentDetail(String serviceName, String componentName) {
      this.serviceName = serviceName;
      this.componentName = componentName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(serviceName, componentName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      ServiceComponentDetail other = (ServiceComponentDetail) obj;
      return Objects.equals(serviceName, other.serviceName)
          && Objects.equals(componentName, other.componentName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(ServiceComponentDetail other) {
      return Comparator.comparing(
          (ServiceComponentDetail detail) -> detail.serviceName).thenComparing(
              detail -> detail.componentName).compare(this, other);
    }
  }

  /**
   * Used to represent information about a host. This class is safe to use in
   * sorted & unique collections.
   */
  static class HostDetail implements Comparable<HostDetail> {
    @JsonProperty("host_id")
    final Long hostId;

    @JsonProperty("host_name")
    final String hostName;

    HostDetail(Long hostId, String hostName) {
      this.hostId = hostId;
      this.hostName = hostName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(hostId, hostName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      HostDetail other = (HostDetail) obj;
      return Objects.equals(hostId, other.hostId) && Objects.equals(hostName, other.hostName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(HostDetail other) {
      return hostName.compareTo(other.hostName);
    }
  }
}
