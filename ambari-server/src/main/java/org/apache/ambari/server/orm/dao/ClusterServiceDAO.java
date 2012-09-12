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

package org.apache.ambari.server.orm.dao;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;

import javax.persistence.EntityManager;

public class ClusterServiceDAO {
  @Inject
  EntityManager entityManager;

  public ClusterServiceEntity findByPK(ClusterServiceEntityPK clusterServiceEntityPK) {
    return entityManager.find(ClusterServiceEntity.class, clusterServiceEntityPK);
  }

  public ClusterServiceEntity findByClusterAndServiceNames(String clusterName, String serviceName) {
    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterName(clusterName);
    pk.setServiceName(serviceName);
    return findByPK(pk);
  }

  @Transactional
  public void create(ClusterServiceEntity clusterServiceEntity) {
    entityManager.persist(clusterServiceEntity);
  }

  @Transactional
  public ClusterServiceEntity merge(ClusterServiceEntity clusterServiceEntity) {
    return entityManager.merge(clusterServiceEntity);
  }

  @Transactional
  public void remove(ClusterServiceEntity clusterServiceEntity) {
    entityManager.remove(clusterServiceEntity);
  }

  @Transactional
  public void removeByPK(ClusterServiceEntityPK clusterServiceEntityPK) {
    remove(findByPK(clusterServiceEntityPK));
  }

}
