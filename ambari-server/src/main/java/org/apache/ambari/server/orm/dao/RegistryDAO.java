/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.apache.ambari.server.orm.entities.RegistryEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Software Registry Data Access Object
 */
@Singleton
public class RegistryDAO {
  protected final static Logger LOG = LoggerFactory.getLogger(RegistryDAO.class);

  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> m_entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils m_daoUtils;

  /**
   * Persists a new software registry
   */
  @Transactional
  public Long create(RegistryEntity registryEntity) {
    m_entityManagerProvider.get().persist(registryEntity);
    return registryEntity.getRegistryId();
  }

  /**
   * Gets all software registries stored in the database.
   *
   * @return all software registries or an empty list if none exist (never {@code null}).
   */
  @RequiresSession
  public List<RegistryEntity> findAll() {
    TypedQuery<RegistryEntity> query = m_entityManagerProvider.get().createNamedQuery(
      "RegistryEntity.findAll", RegistryEntity.class);
    return m_daoUtils.selectList(query);
  }

  /**
   * Gets a software registry with the specified ID.
   *
   * @param registryId
   *          the ID of the software registry to be retrieved.
   * @return the software registry or {@code null} if none exists.
   */
  @RequiresSession
  public RegistryEntity findById(Long registryId) {
    return m_entityManagerProvider.get().find(RegistryEntity.class, registryId);
  }

  /**
   * Gets software registry with specified mpack name and mpack version.
   *
   * @param registryName the software registry name
   * @return the software registry or {@code null} if none exists.
   */
  @RequiresSession
  public RegistryEntity findByName(String registryName) {
    TypedQuery<RegistryEntity> query = m_entityManagerProvider.get().createNamedQuery(
      "RegistryEntity.findByName", RegistryEntity.class);
    query.setParameter("registryName", registryName);
    return m_daoUtils.selectSingle(query);
  }
}
