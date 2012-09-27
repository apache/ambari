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
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.EntityManager;

public class ClusterDAO {
  private static final Log log = LogFactory.getLog(ClusterDAO.class);

  @Inject
  Provider<EntityManager> entityManagerProvider;

  public ClusterEntity findByName(String clusterName) {
    return entityManagerProvider.get().find(ClusterEntity.class, clusterName);
  }

  @Transactional
  public void create(ClusterEntity clusterEntity) {
    entityManagerProvider.get().persist(clusterEntity);
  }

  @Transactional
  public ClusterEntity merge(ClusterEntity clusterEntity) {
    return entityManagerProvider.get().merge(clusterEntity);
  }

  @Transactional
  public void remove(ClusterEntity clusterEntity) {
    entityManagerProvider.get().remove(clusterEntity);
  }

  @Transactional
  public void removeByName(String clusterName) {
    remove(findByName(clusterName));
  }

}
