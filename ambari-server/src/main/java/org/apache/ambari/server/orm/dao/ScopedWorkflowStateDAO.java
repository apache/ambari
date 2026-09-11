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
import java.util.function.UnaryOperator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ScopedWorkflowStateEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ScopedWorkflowStateDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @RequiresSession
  public ScopedWorkflowStateEntity findByKey(String scopeKey) {
    return entityManagerProvider.get().find(ScopedWorkflowStateEntity.class, scopeKey);
  }

  @RequiresSession
  public List<ScopedWorkflowStateEntity> findActiveCreationDrafts(
      int ownerUserId, String scopePrefix, int limit) {
    return entityManagerProvider.get().createNamedQuery(
        "ScopedWorkflowStateEntity.findActiveCreationDrafts", ScopedWorkflowStateEntity.class)
        .setParameter("ownerUserId", ownerUserId)
        .setParameter("scopePrefix", scopePrefix + "%")
        .setMaxResults(limit)
        .getResultList();
  }

  /**
   * Applies a revision check and replacement while holding the database row
   * lock. New rows are flushed so concurrent creators observe the primary-key
   * conflict before this transaction returns.
   */
  @Transactional
  public ScopedWorkflowStateEntity updateWithLock(String scopeKey,
      UnaryOperator<ScopedWorkflowStateEntity> update) {
    EntityManager entityManager = entityManagerProvider.get();
    ScopedWorkflowStateEntity entity = entityManager.find(
        ScopedWorkflowStateEntity.class, scopeKey, LockModeType.PESSIMISTIC_WRITE);
    boolean create = entity == null;
    if (create) {
      entity = new ScopedWorkflowStateEntity();
      entity.setScopeKey(scopeKey);
      entity.setRevision(0L);
    }

    ScopedWorkflowStateEntity updated = update.apply(entity);
    if (updated == null || !scopeKey.equals(updated.getScopeKey())) {
      throw new IllegalArgumentException("Scoped workflow updates must retain their storage key");
    }
    if (create) {
      entityManager.persist(updated);
    }
    entityManager.flush();
    return updated;
  }
}
