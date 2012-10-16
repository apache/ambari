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
import org.apache.ambari.server.orm.entities.ServiceComponentConfigEntity;

import javax.persistence.EntityManager;

public class ServiceComponentConfigDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  public ServiceComponentConfigEntity findByPK(Integer primaryKey) {
    return entityManagerProvider.get().find(ServiceComponentConfigEntity.class, primaryKey);
  }

  public void refresh(ServiceComponentConfigEntity serviceComponentConfigEntity) {
    entityManagerProvider.get().refresh(serviceComponentConfigEntity);
  }

  @Transactional
  public void create(ServiceComponentConfigEntity serviceComponentConfigEntity) {
    entityManagerProvider.get().persist(serviceComponentConfigEntity);
  }

  @Transactional
  public ServiceComponentConfigEntity merge(ServiceComponentConfigEntity serviceComponentConfigEntity) {
    return entityManagerProvider.get().merge(serviceComponentConfigEntity);
  }

  @Transactional
  public void remove(ServiceComponentConfigEntity serviceComponentConfigEntity) {
    entityManagerProvider.get().remove(serviceComponentConfigEntity);
  }

  @Transactional
  public void removeByPK(Integer primaryKey) {
    remove(findByPK(primaryKey));
  }

}
