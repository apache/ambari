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

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.PrereqCheckType;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Describes prerequisite check.
 */
public abstract class AbstractCheckDescriptor {

  public final String id;
  public final String description;
  public final PrereqCheckType type;

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

  /**
   * Constructor.
   *
   * @param id unique identifier
   * @param type type
   * @param description description
   */
  public AbstractCheckDescriptor(String id, PrereqCheckType type, String description) {
    this.id = id;
    this.type = type;
    this.description = description;
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
    if (type != null) {
      formatted.append(" ").append(type.name().toLowerCase());
      if (entities.size() > 1) {
        formatted.append("s");
      }
    }
    return formatted.toString();
  }

}
