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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.MpackRecordEntity;
import org.apache.ambari.server.orm.entities.MpackRecordEntity.Kind;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/** Applies related inventory, idempotency and operation changes in one transaction. */
@Singleton
public class MpackRecordDAO {
  public static final int SCHEMA_VERSION = 1;
  public static final long ABSENT = -1;

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  public record Value(String id, Kind kind, long revision, int schemaVersion, String payload) {
  }

  public record Change(String id, Kind kind, long expectedRevision, String payload) {
    public Change {
      if (id == null || id.isEmpty() || id.length() > 255 || kind == null
          || expectedRevision < ABSENT || payload == null || payload.isEmpty()) {
        throw new IllegalArgumentException("Invalid management pack record change");
      }
    }
  }

  @RequiresSession
  public Value find(String id) {
    MpackRecordEntity entity = entityManagerProvider.get().find(MpackRecordEntity.class, id);
    return entity == null ? null : value(entity);
  }

  @RequiresSession
  public List<Value> list(Kind kind) {
    return entityManagerProvider.get().createQuery(
        "SELECT record FROM MpackRecordEntity record WHERE record.kind=:kind ORDER BY record.id",
        MpackRecordEntity.class).setParameter("kind", kind).getResultList().stream()
        .map(MpackRecordDAO::value).toList();
  }

  /**
   * Existing records are locked in stable order. Missing records are protected
   * by their primary key; a concurrent creator fails the entire transaction.
   * Callers include the control revision when a change depends on the catalog.
   */
  @Transactional
  public List<Value> apply(List<Change> changes) {
    EntityManager manager = entityManagerProvider.get();
    List<Change> ordered = new ArrayList<>(changes);
    ordered.sort(Comparator.comparing(Change::id));
    Set<String> ids = new HashSet<>();
    List<Value> result = new ArrayList<>();
    for (Change change : ordered) {
      if (!ids.add(change.id())) {
        throw new IllegalArgumentException("Duplicate management pack record change");
      }
      MpackRecordEntity entity = manager.find(MpackRecordEntity.class, change.id(),
          LockModeType.PESSIMISTIC_WRITE);
      if (entity != null) {
        manager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
      }
      long actual = entity == null ? ABSENT : entity.getRevision();
      if (actual != change.expectedRevision()) {
        throw new OptimisticLockException("Management pack record revision changed: " + change.id());
      }
      if (entity != null && (entity.getKind() != change.kind()
          || entity.getSchemaVersion() != SCHEMA_VERSION)) {
        throw new IllegalStateException("Management pack record contract mismatch: " + change.id());
      }
      boolean create = entity == null;
      if (create) {
        entity = new MpackRecordEntity();
        entity.setId(change.id());
        entity.setKind(change.kind());
        entity.setSchemaVersion(SCHEMA_VERSION);
      }
      entity.setRevision(Math.addExact(actual, 1));
      entity.setPayload(change.payload());
      if (create) {
        manager.persist(entity);
      }
      result.add(value(entity));
    }
    manager.flush();
    return List.copyOf(result);
  }

  private static Value value(MpackRecordEntity entity) {
    if (entity.getSchemaVersion() == null || entity.getSchemaVersion() != SCHEMA_VERSION
        || entity.getRevision() == null || entity.getRevision() < 0 || entity.getKind() == null
        || entity.getPayload() == null) {
      throw new IllegalStateException("Invalid management pack record: " + entity.getId());
    }
    return new Value(entity.getId(), entity.getKind(), entity.getRevision(),
        entity.getSchemaVersion(), entity.getPayload());
  }
}
