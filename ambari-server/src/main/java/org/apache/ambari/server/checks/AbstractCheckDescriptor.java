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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Describes prerequisite check.
 */
public abstract class AbstractCheckDescriptor {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractCheckDescriptor.class);

  protected static final String DEFAULT = "default";

  @Inject
  Provider<Clusters> clustersProvider;

  @Inject
  Provider<ClusterVersionDAO> clusterVersionDAOProvider;

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
   * Tests if the prerequisite check is applicable to given cluster. This
   * method's default logic is to ensure that the cluster stack source and
   * target are compatible with the prerequisite check. When overridding this
   * method, call {@code super#isApplicable(PrereqCheckRequest)}.
   *
   * @param request
   *          prerequisite check request
   * @return true if check should be performed
   *
   * @throws org.apache.ambari.server.AmbariException
   *           if server error happens
   */
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
    // this is default behaviour
   return true;
  }

  /**
   * Same like {@code isApplicable(PrereqCheckRequest request)}, but with service presence check
   * @param request
   *          prerequisite check request
   * @param requiredServices
   *          set of services, which need to be present to allow check execution
   * @param requiredAll
   *          require all services in the list or at least one need to present
   * @return true if check should be performed
   * @throws org.apache.ambari.server.AmbariException
   *           if server error happens
   */
  public boolean isApplicable(PrereqCheckRequest request, List<String> requiredServices, boolean requiredAll) throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    Set<String> services = cluster.getServices().keySet();

    // default return value depends on assign inside check block
    boolean serviceFound = requiredAll && !requiredServices.isEmpty();

    for (String service : requiredServices) {
      if ( services.contains(service) && !requiredAll) {
        serviceFound = true;
        break;
      } else if (!services.contains(service) && requiredAll) {
        serviceFound = false;
        break;
      }
    }

    return serviceFound;
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
  protected String getFailReason(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) {
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
  protected String getFailReason(String key,
      PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) {
    String fail = m_description.getFail(key);

    if (fail.contains("{{version}}") && null != request.getRepositoryVersion()) {
      fail = fail.replace("{{version}}", request.getRepositoryVersion());
    }

    if (fail.contains("{{fails}}")) {
      LinkedHashSet<String> names = prerequisiteCheck.getFailedOn();

      // If Type=PrereqCheckType.HOST, names list is already populated
      if (getDescription().getType() == PrereqCheckType.SERVICE) {
        Clusters clusters = clustersProvider.get();
        AmbariMetaInfo metaInfo = ambariMetaInfo.get();

        try {
          Cluster c = clusters.getCluster(request.getClusterName());
          Map<String, ServiceInfo> services = metaInfo.getServices(
              c.getDesiredStackVersion().getStackName(),
              c.getDesiredStackVersion().getStackVersion());

          LinkedHashSet<String> displays = new LinkedHashSet<String>();
          for (String name : names) {
            if (services.containsKey(name)) {
              displays.add(services.get(name).getDisplayName());
            } else {
              displays.add(name);
            }
          }
          names = displays;
        } catch (Exception e) {
          LOG.warn("Could not load service info map");
        }
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
   * Return a boolean indicating whether or not configs allow bypassing errors during the RU/EU PreChecks.
   * @return
   */
  public boolean isStackUpgradeAllowedToBypassPreChecks() {
    return config.isUpgradePrecheckBypass();
  }
}
