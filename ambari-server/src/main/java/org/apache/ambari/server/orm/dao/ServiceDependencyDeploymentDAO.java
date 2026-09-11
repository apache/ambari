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
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyDeploymentEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/** Serializes deployment publication with its actual Ambari request transaction. */
@Singleton
public class ServiceDependencyDeploymentDAO {
  @Inject
  private Provider<EntityManager> entityManagers;

  @RequiresSession
  public ServiceDependencyDeploymentEntity find(String id) {
    return entityManagers.get().find(ServiceDependencyDeploymentEntity.class, id);
  }

  @RequiresSession
  public List<ServiceDependencyDeploymentEntity> active(int offset, int limit) {
    return entityManagers.get().createQuery(
        "SELECT d FROM ServiceDependencyDeploymentEntity d WHERE d.state IN "
            + "('NEW','WAIT_PROVIDER','INSTALLING','WAIT_DEPENDENCIES','STARTING','CHECKING') "
            + "ORDER BY d.deploymentId", ServiceDependencyDeploymentEntity.class)
        .setFirstResult(offset).setMaxResults(limit).getResultList();
  }

  @Transactional
  public ServiceDependencyDeploymentEntity create(ServiceDependencyDeploymentEntity proposed) {
    EntityManager em = entityManagers.get();
    if (em.find(ClusterEntity.class, proposed.getClusterId(), LockModeType.PESSIMISTIC_WRITE) == null) {
      throw new IllegalStateException("The deployment cluster no longer exists");
    }
    ServiceDependencyDeploymentEntity existing = em.find(ServiceDependencyDeploymentEntity.class,
        proposed.getDeploymentId(), LockModeType.PESSIMISTIC_WRITE);
    if (existing != null) {
      if (!Objects.equals(existing.getClusterId(), proposed.getClusterId())
          || !Objects.equals(existing.getOwnerUserId(), proposed.getOwnerUserId())
          || !Objects.equals(existing.getPlanJson(), proposed.getPlanJson())) {
        throw new IllegalArgumentException("The deployment ID already belongs to a different intent");
      }
      return existing;
    }
    // A consumer deployment owns the selected service installation until it is resolved.
    List<ServiceDependencyDeploymentEntity> unresolved = em.createQuery(
        "SELECT d FROM ServiceDependencyDeploymentEntity d WHERE d.clusterId=:clusterId "
            + "AND d.state NOT IN ('COMPLETE','INSTALL_ONLY')", ServiceDependencyDeploymentEntity.class)
        .setParameter("clusterId", proposed.getClusterId()).getResultList();
    if (!unresolved.isEmpty()) {
      throw new IllegalStateException("Resume the existing managed deployment before creating another");
    }
    em.persist(proposed);
    em.flush();
    return proposed;
  }

  @Transactional(rollbackOn = {Exception.class})
  public ServiceDependencyDeploymentEntity mutate(String id, Mutation mutation) throws AmbariException {
    EntityManager em = entityManagers.get();
    ServiceDependencyDeploymentEntity value = em.find(ServiceDependencyDeploymentEntity.class,
        id, LockModeType.PESSIMISTIC_WRITE);
    if (value == null) {
      throw new AmbariException("The deployment no longer exists");
    }
    try {
      mutation.apply(value);
      value.setUpdateTimestamp(System.currentTimeMillis());
      em.flush();
      return value;
    } catch (Exception e) {
      if (em.getTransaction().isActive()) {
        em.getTransaction().setRollbackOnly();
      }
      if (e instanceof AmbariException) {
        throw (AmbariException) e;
      }
      throw new AmbariException("Deployment publication failed", e);
    }
  }

  @FunctionalInterface
  public interface Mutation {
    void apply(ServiceDependencyDeploymentEntity value) throws Exception;
  }
}
