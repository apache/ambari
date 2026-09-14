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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import org.apache.ambari.server.controller.dependencies.ManagedDependencySnapshot;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.CreationGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.DraftGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.RepositoryGuard;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO.StaleApprovalException;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ScopedWorkflowStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyOperationEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.inject.Provider;

class ServiceDependencyDAOTest {
  private EntityManager entityManager;
  private ServiceDependencyDAO dao;
  private ServiceDependencyBindingEntity binding;
  private ServiceDependencySnapshotEntity snapshot;
  private ServiceDependencyOperationEntity operation;

  @BeforeEach
  void setUp() throws Exception {
    entityManager = mock(EntityManager.class);
    dao = new ServiceDependencyDAO();
    Field providerField = ServiceDependencyDAO.class.getDeclaredField("entityManagerProvider");
    providerField.setAccessible(true);
    providerField.set(dao, (Provider<EntityManager>) () -> entityManager);

    binding = new ServiceDependencyBindingEntity();
    binding.setBindingId("a33171de-0fea-4c21-8bd5-26e8f4de9188");
    binding.setConsumerClusterId(11L);
    binding.setConsumerServiceName("HBASE");
    binding.setProviderClusterId(23L);
    binding.setProviderServiceName("HDFS");
    binding.setDependencyType("HDFS");
    binding.setOperationEpoch(1L);
    binding.setDesiredSnapshotVersion(1L);
    binding.setActiveOperationId("create-operation");
    snapshot = new ServiceDependencySnapshotEntity();
    snapshot.setBindingId(binding.getBindingId());
    snapshot.setSnapshotVersion(1L);
    snapshot.setSchemaVersion(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION);
    snapshot.setConsumerServiceVersion("3.3.0");
    operation = new ServiceDependencyOperationEntity();
    operation.setOperationId("create-operation");
    operation.setBindingId(binding.getBindingId());
    operation.setOperationKind("CREATE");
    operation.setOperationEpoch(1L);
    operation.setTargetSnapshotVersion(1L);
    operation.setRequestHash("request-hash");

    when(entityManager.find(eq(ClusterServiceEntity.class), any(ClusterServiceEntityPK.class),
        eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(new ClusterServiceEntity());
    TypedQuery<ServiceDependencyBindingEntity> existingBindingQuery = mock(TypedQuery.class);
    when(entityManager.createNamedQuery(
        "ServiceDependencyBindingEntity.findByConsumerAndType", ServiceDependencyBindingEntity.class))
        .thenReturn(existingBindingQuery);
    when(existingBindingQuery.setParameter(anyString(), any())).thenReturn(existingBindingQuery);
    when(existingBindingQuery.setLockMode(any(LockModeType.class))).thenReturn(existingBindingQuery);
    when(existingBindingQuery.setMaxResults(anyInt())).thenReturn(existingBindingQuery);
    when(existingBindingQuery.getResultList()).thenReturn(List.of());
  }

  @Test
  void staleDraftCannotPublishAnyDependencyRows() {
    ScopedWorkflowStateEntity current = new ScopedWorkflowStateEntity();
    current.setOwnerUserId(7);
    current.setRevision(4L);
    current.setCreatedClusterId(11L);
    current.setWorkflow("CLUSTER_CREATE");
    when(entityManager.find(ScopedWorkflowStateEntity.class, "draft:7",
        LockModeType.PESSIMISTIC_WRITE)).thenReturn(current);

    CreationGuard guard = new CreationGuard(
        new DraftGuard("draft:7", 7, 3L, 11L), List.of(), List.of());

    assertThrows(StaleApprovalException.class,
        () -> dao.create(binding, snapshot, operation, guard));
    verify(entityManager, never()).persist(any());
  }

  @Test
  void changedRepositoryCannotPublishAnyDependencyRows() {
    RepositoryVersionEntity current = new RepositoryVersionEntity();
    current.setId(91L);
    current.setVersion("3.3.0-2");
    current.setResolved(true);
    when(entityManager.find(RepositoryVersionEntity.class, 91L,
        LockModeType.PESSIMISTIC_READ)).thenReturn(current);

    CreationGuard guard = new CreationGuard(null,
        List.of(new RepositoryGuard(91L, "3.3.0-1", true)), List.of());

    assertThrows(StaleApprovalException.class,
        () -> dao.create(binding, snapshot, operation, guard));
    verify(entityManager, never()).persist(any());
  }
}
