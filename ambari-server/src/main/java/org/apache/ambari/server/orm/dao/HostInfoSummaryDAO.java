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

package org.apache.ambari.server.orm.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class HostInfoSummaryDAO {

  private static final Logger LOG = LoggerFactory.getLogger(HostInfoSummaryDAO.class);

  // Whenever we want to aggregate more host info in summary, we need to update these two statements
  // also we must update HostInfoSummary.java
  private static final String SUMMARY_WITH_CLUSTER_NAME_QUERY_STATEMENT =
          "SELECT NEW %s(" +
          "host.osType, COUNT(host.osType)) " +
          "FROM ClusterHostMappingEntity clusters " +
          "JOIN ClusterEntity cluster ON clusters.clusterId = cluster.clusterId " +
          "JOIN HostEntity host ON clusters.hostId = host.hostId " +
          "WHERE cluster.clusterName = :cluster_name " +
          "GROUP BY host.osType";

  private static final String SUMMARY_WITHOUT_CLUSTER_NAME_QUERY_STATEMENT =
      "SELECT NEW %s(" +
          "host.osType, COUNT(host.osType)) " +
          "FROM ClusterHostMappingEntity clusters " +
          "JOIN HostEntity host ON clusters.hostId = host.hostId " +
          "GROUP BY host.osType";

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @Inject
  private DaoUtils daoUtils;

  @RequiresSession
  public List<HostInfoSummaryDTO> findHostInfoSummary(String clusterName) {
    String sql = String.format(StringUtils.isBlank(clusterName) ? SUMMARY_WITHOUT_CLUSTER_NAME_QUERY_STATEMENT : SUMMARY_WITH_CLUSTER_NAME_QUERY_STATEMENT, HostInfoSummaryDTO.class.getName());

    TypedQuery<HostInfoSummaryDTO> query = entityManagerProvider.get().createQuery(sql, HostInfoSummaryDTO.class);
    if (StringUtils.isNotBlank(clusterName)) {
      query.setParameter("cluster_name", clusterName);
    }
    return daoUtils.selectList(query);
  }

}
