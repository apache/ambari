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
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;

import javax.persistence.EntityManager;

@Singleton
public class HostComponentDesiredStateDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Transactional
  public HostComponentDesiredStateEntity findByPK(HostComponentDesiredStateEntityPK primaryKey) {
    return entityManagerProvider.get().find(HostComponentDesiredStateEntity.class, primaryKey);
  }

  @Transactional
  public void refresh(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    entityManagerProvider.get().refresh(hostComponentDesiredStateEntity);
  }

  @Transactional
  public void create(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    entityManagerProvider.get().persist(hostComponentDesiredStateEntity);
  }

  @Transactional
  public HostComponentDesiredStateEntity merge(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    return entityManagerProvider.get().merge(hostComponentDesiredStateEntity);
  }

  @Transactional
  public void remove(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    entityManagerProvider.get().remove(merge(hostComponentDesiredStateEntity));
  }

  @Transactional
  public void removeByPK(HostComponentDesiredStateEntityPK primaryKey) {
    remove(findByPK(primaryKey));
  }

}
