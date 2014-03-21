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
import org.apache.ambari.server.orm.entities.UserEntity;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;
import org.apache.ambari.server.orm.entities.RoleEntity;

@Singleton
public class UserDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public UserEntity findByPK(Integer userPK) {
    return entityManagerProvider.get().find(UserEntity.class, userPK);
  }

  @RequiresSession
  public List<UserEntity> findAll() {
    TypedQuery<UserEntity> query = entityManagerProvider.get().createQuery("SELECT user FROM UserEntity user", UserEntity.class);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<UserEntity> findAllLocalUsersByRole(RoleEntity roleEntity) {
    TypedQuery<UserEntity> query = entityManagerProvider.get().createQuery("SELECT role.userEntities FROM RoleEntity role WHERE role = :roleEntity", UserEntity.class);
    query.setParameter("roleEntity", roleEntity);
    return query.getResultList();
  }

  @RequiresSession
  public UserEntity findLocalUserByName(String userName) {
    TypedQuery<UserEntity> query = entityManagerProvider.get().createNamedQuery("localUserByName", UserEntity.class);
    query.setParameter("username", userName.toLowerCase());
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @RequiresSession
  public UserEntity findLdapUserByName(String userName) {
    TypedQuery<UserEntity> query = entityManagerProvider.get().createNamedQuery("ldapUserByName", UserEntity.class);
    query.setParameter("username", userName.toLowerCase());
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @Transactional
  public void create(UserEntity user) {
    user.setUserName(user.getUserName().toLowerCase());
    entityManagerProvider.get().persist(user);
  }

  @Transactional
  public UserEntity merge(UserEntity user) {
    user.setUserName(user.getUserName().toLowerCase());
    return entityManagerProvider.get().merge(user);
  }

  @Transactional
  public void remove(UserEntity user) {
    entityManagerProvider.get().remove(merge(user));
  }

  @Transactional
  public void removeByPK(Integer userPK) {
    remove(findByPK(userPK));
  }

}
