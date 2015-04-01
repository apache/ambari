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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
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
  Provider<HostVersionDAO> hostVersionDaoProvider;

  @Inject
  Provider<RepositoryVersionDAO> repositoryVersionDaoProvider;

  @Inject
  Provider<RepositoryVersionHelper> repositoryVersionHelper;

  @Inject
  Provider<AmbariMetaInfo> ambariMetaInfo;

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
   * Tests if the prerequisite check is applicable to given cluster. By default returns true.
   *
   * @param request prerequisite check request
   * @return true if check should be performed
   *
   * @throws org.apache.ambari.server.AmbariException if server error happens
   */
  public boolean isApplicable(PrereqCheckRequest request) throws AmbariException {
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


  public CheckDescription getDescription() {
    return m_description;
  }

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
      List<String> names = prerequisiteCheck.getFailedOn();

      // If Type=PrereqCheckType.HOST, names list is already populated
      if (getDescription().getType() == PrereqCheckType.SERVICE) {
        Clusters clusters = clustersProvider.get();
        AmbariMetaInfo metaInfo = ambariMetaInfo.get();

        try {
          Cluster c = clusters.getCluster(request.getClusterName());
          Map<String, ServiceInfo> services = metaInfo.getServices(
              c.getDesiredStackVersion().getStackName(),
              c.getDesiredStackVersion().getStackVersion());

          List<String> displays = new ArrayList<String>();
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
  protected String formatEntityList(List<String> entities) {
    if (entities == null || entities.isEmpty()) {
      return "";
    }
    final StringBuilder formatted = new StringBuilder(StringUtils.join(entities, ", "));
    if (entities.size() > 1) {
      formatted.replace(formatted.lastIndexOf(","), formatted.lastIndexOf(",") + 1, " and");
    }

    return formatted.toString();
  }

}
