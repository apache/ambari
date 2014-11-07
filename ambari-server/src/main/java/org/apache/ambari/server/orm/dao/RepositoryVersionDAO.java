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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * DAO for repository versions.
 *
 */
@Singleton
public class RepositoryVersionDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Retrieves repository version by primary key.
   *
   * @param repositoryVersionPK primary key
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public RepositoryVersionEntity findByPK(Long repositoryVersionPK) {
    return entityManagerProvider.get().find(RepositoryVersionEntity.class, repositoryVersionPK);
  }

  /**
   * Retrieves all repository versions.
   *
   * @return list of all repository versions
   */
  @RequiresSession
  public List<RepositoryVersionEntity> findAll() {
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createQuery("SELECT repoversion FROM RepositoryVersionEntity repoversion", RepositoryVersionEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * Retrieves repository version by name.
   *
   * @param displayName display name
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public RepositoryVersionEntity findByDisplayName(String displayName) {
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery("repositoryVersionByDisplayName", RepositoryVersionEntity.class);
    query.setParameter("displayname", displayName);
    return daoUtils.selectSingle(query);
  }

  /**
   * Retrieves repository version by stack and version.
   *
   * @param stack stack
   * @param version version
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public RepositoryVersionEntity findByStackAndVersion(String stack, String version) {
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery("repositoryVersionByStackVersion", RepositoryVersionEntity.class);
    query.setParameter("stack", stack);
    query.setParameter("version", version);
    return daoUtils.selectSingle(query);
  }

  /**
   * Create repository version.
   *
   * @param repositoryVersion entity to create
   */
  @Transactional
  public void create(RepositoryVersionEntity repositoryVersion) {
    entityManagerProvider.get().persist(repositoryVersion);
  }

  /**
   * Update repository version.
   *
   * @param repositoryVersion entity to update
   * @return updated repository version
   */
  @Transactional
  public RepositoryVersionEntity merge(RepositoryVersionEntity repositoryVersion) {
    return entityManagerProvider.get().merge(repositoryVersion);
  }

  /**
   * Deletes repository version.
   *
   * @param repositoryVersion entity to delete
   */
  @Transactional
  public void remove(RepositoryVersionEntity repositoryVersion) {
    entityManagerProvider.get().remove(merge(repositoryVersion));
  }
}
