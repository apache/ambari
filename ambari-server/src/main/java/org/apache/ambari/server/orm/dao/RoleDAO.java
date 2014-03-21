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
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RoleEntity;

import javax.persistence.EntityManager;
import java.util.List;

@Singleton
public class RoleDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public RoleEntity findByName(String roleName) {
    return entityManagerProvider.get().find(RoleEntity.class, roleName.toLowerCase());
  }

  @RequiresSession
  public List<RoleEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), RoleEntity.class);
  }

  @Transactional
  public void create(RoleEntity role) {
    role.setRoleName(role.getRoleName().toLowerCase());
    entityManagerProvider.get().persist(role);
  }

  @Transactional
  public RoleEntity merge(RoleEntity role) {
    return entityManagerProvider.get().merge(role);
  }

  @Transactional
  public void remove(RoleEntity role) {
    entityManagerProvider.get().remove(merge(role));
  }

  @Transactional
  public void removeByName(String roleName) {
    remove(findByName(roleName));
  }

}
