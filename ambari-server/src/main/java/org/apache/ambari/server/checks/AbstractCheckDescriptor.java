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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Describes prerequisite check.
 */
public abstract class AbstractCheckDescriptor {

  protected static final String DEFAULT = "default";

  @Inject
  protected Provider<Clusters> clustersProvider;

  @Inject
  Provider<HostVersionDAO> hostVersionDaoProvider;

  @Inject
  Provider<RepositoryVersionDAO> repositoryVersionDaoProvider;

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

  private CheckDescription m_description;

  /**
   * Constructor.
   *
   * @param description description
   */
  protected AbstractCheckDescriptor(CheckDescription description) {
    m_description = description;
  }

  /**
   * Gets the set of services that this check is associated with. If the check
   * is not associated with a particular service, then this should be an empty
   * set.
   *
   * @return a set of services which will determine whether this check is
   *         applicable.
   */
  public Set<String> getApplicableServices() {
    return Collections.emptySet();
  }

  /**
   * Gets any additional qualifications which an upgrade check should run in
   * order to determine if it's applicable to the upgrade.
   *
   * @return a list of qualifications, or an empty list.
   */
  public List<CheckQualification> getQualifications() {
    return Collections.emptyList();
  }

  /**
   * Tests if the prerequisite check is applicable to given upgrade request. If
   * a check requires some extra processing
   *
   * @param request
   *          prerequisite check request
   * @return true if check should be performed
   *
   * @throws org.apache.ambari.server.AmbariException
   *           if server error happens
   */
  public final boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    List<CheckQualification> qualifications = Lists.newArrayList(
        new ServiceQualification(), new OrchestrationQualification(getClass()));

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
   * Executes check against given cluster.
   *
   * @param prerequisiteCheck dto for upgrade check results
   * @param request pre upgrade check request
   *
   * @throws AmbariException if server error happens
   */
  public abstract void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException;


  /**
   * Gets the description of the check.
   *
   * @return the description (not {@code null}).
   */
  public CheckDescription getDescription() {
    return m_description;
  }

  /**
   * Gets the type of check.
   *
   * @return the type of check (not {@code null}).
   */
  public PrereqCheckType getType() {
    return m_description.getType();
  }

  /**
   * Gets the default fail reason
   * @param prerequisiteCheck the check being performed
   * @param request           the request
   * @return the failure string
   */
  protected String getFailReason(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request)
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
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
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
  protected String getFailReason(String key, PrerequisiteCheck prerequisiteCheck,
      PrereqCheckRequest request) throws AmbariException {
    String fail = m_description.getFail(key);

    RepositoryVersionEntity repositoryVersion = request.getTargetRepositoryVersion();
    if (fail.contains("{{version}}") && null != repositoryVersion) {
      fail = fail.replace("{{version}}", repositoryVersion.getVersion());
    }

    if (fail.contains("{{fails}}")) {
      LinkedHashSet<String> names = prerequisiteCheck.getFailedOn();

      // If Type=PrereqCheckType.HOST, names list is already populated
      if (getDescription().getType() == PrereqCheckType.SERVICE) {
        Clusters clusters = clustersProvider.get();
        AmbariMetaInfo metaInfo = ambariMetaInfo.get();

        Cluster c = clusters.getCluster(request.getClusterName());
        Map<String, ServiceInfo> services = metaInfo.getServices(
            c.getDesiredStackVersion().getStackName(),
            c.getDesiredStackVersion().getStackVersion());

        LinkedHashSet<String> displays = new LinkedHashSet<>();
        for (String name : names) {
          if (services.containsKey(name)) {
            displays.add(services.get(name).getDisplayName());
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
   * Gets whether this upgrade check is required for the specified
   * {@link UpgradeType}. Checks which are marked as required do not need to be
   * explicitely declared in the {@link UpgradePack} to be run.
   *
   * @return {@code true} if it is required, {@code false} otherwise.
   */
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
   * Gets a de-serialized {@link VersionDefinitionXml} from the repository for
   * this upgrade.
   *
   * @param request
   *          the upgrade check request.
   * @return the VDF XML
   * @throws AmbariException
   */
  final VersionDefinitionXml getVersionDefinitionXml(PrereqCheckRequest request) throws AmbariException {
    RepositoryVersionEntity repositoryVersion = request.getTargetRepositoryVersion();

    try {
      VersionDefinitionXml vdf = repositoryVersion.getRepositoryXml();
      return vdf;
    } catch (Exception exception) {
      throw new AmbariException("Unable to run upgrade checks because of an invalid VDF",
          exception);
    }
  }

  /**
   * Gets the services participating in the upgrade from the VDF.
   *
   * @param request
   *          the upgrade check request.
   * @return the services participating in the upgrade, which can either be all
   *         of the cluster's services or a subset based on repository type.
   */
  final Set<String> getServicesInUpgrade(PrereqCheckRequest request) throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());

    // the check is scoped to some services, so determine if any of those
    // services are included in this upgrade
    try {
      VersionDefinitionXml vdf = getVersionDefinitionXml(request);
      ClusterVersionSummary clusterVersionSummary = vdf.getClusterSummary(cluster);
      return clusterVersionSummary.getAvailableServiceNames();
    } catch (Exception exception) {
      throw new AmbariException("Unable to run upgrade checks because of an invalid VDF",
          exception);
    }
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
   * cluster and included in thr upgrade.
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

      Set<String> servicesForUpgrade = getServicesInUpgrade(request);

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
   * The {@link OrchestrationQualification} class is used to determine if the
   * check is required to run based on the {@link RepositoryType}.
   */
  final class OrchestrationQualification implements CheckQualification {

    private final Class<? extends AbstractCheckDescriptor> m_checkClass;

    /**
     * Constructor.
     *
     * @param checkClass
     *          the class of the check which is being considered for
     *          applicability.
     */
    public OrchestrationQualification(Class<? extends AbstractCheckDescriptor> checkClass) {
      m_checkClass = checkClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
      RepositoryVersionEntity repositoryVersion = request.getTargetRepositoryVersion();
      RepositoryType repositoryType = repositoryVersion.getType();

      UpgradeCheck annotation = m_checkClass.getAnnotation(UpgradeCheck.class);
      if (null == annotation) {
        return true;
      }

      RepositoryType[] repositoryTypes = annotation.orchestration();

      if (ArrayUtils.isEmpty(repositoryTypes)
          || ArrayUtils.contains(repositoryTypes, repositoryType)) {
        return true;
      }

      return false;
    }
  }
}
