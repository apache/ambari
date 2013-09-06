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
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntityPK;

import javax.persistence.EntityManager;

@Singleton
public class ServiceDesiredStateDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Transactional
  public ServiceDesiredStateEntity findByPK(ServiceDesiredStateEntityPK primaryKey) {
    return entityManagerProvider.get().find(ServiceDesiredStateEntity.class, primaryKey);
  }

  @Transactional
  public void refresh(ServiceDesiredStateEntity serviceDesiredStateEntity) {
    entityManagerProvider.get().refresh(serviceDesiredStateEntity);
  }

  @Transactional
  public void create(ServiceDesiredStateEntity serviceDesiredStateEntity) {
    entityManagerProvider.get().persist(serviceDesiredStateEntity);
  }

  @Transactional
  public ServiceDesiredStateEntity merge(ServiceDesiredStateEntity serviceDesiredStateEntity) {
    return entityManagerProvider.get().merge(serviceDesiredStateEntity);
  }

  @Transactional
  public void remove(ServiceDesiredStateEntity serviceDesiredStateEntity) {
    entityManagerProvider.get().remove(merge(serviceDesiredStateEntity));
  }

  @Transactional
  public void removeByPK(ServiceDesiredStateEntityPK primaryKey) {
    remove(findByPK(primaryKey));
  }

}
