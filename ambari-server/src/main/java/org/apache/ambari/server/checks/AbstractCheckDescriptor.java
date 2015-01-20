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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.PrereqCheckType;

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
  Provider<Configuration> configurationProvider;

  @Inject
  Provider<HostVersionDAO> hostVersionDaoProvider;

  @Inject
  Provider<RepositoryVersionDAO> repositoryVersionDaoProvider;

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
}