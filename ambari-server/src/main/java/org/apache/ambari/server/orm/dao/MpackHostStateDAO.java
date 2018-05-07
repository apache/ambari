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

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.MpackHostStateEntity;
import org.apache.ambari.server.state.MpackInstallState;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link MpackHostStateDAO} contains all of the CRUD operations relating to
 * the installation state of a management pack on a host.
 */
@Singleton
@Experimental(feature = ExperimentalFeature.UNIT_TEST_REQUIRED)
public class MpackHostStateDAO extends CrudDAO<MpackHostStateEntity, Long> {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  /**
   * Constructor.
   */
  public MpackHostStateDAO() {
    super(MpackHostStateEntity.class);
  }

  /**
   * @param entity entity to create
   */
  @Override
  @Transactional
  public void create(MpackHostStateEntity entity) throws IllegalArgumentException {
    super.create(entity);
  }

  /**
   * Retrieve all of the install states for any management packs installed on
   * the specified host.
   *
   * @param hostName
   *          FQDN of host
   * @return Return all of the mpack install states that match the criteria.
   */
  @RequiresSession
  public List<MpackHostStateEntity> findByHost(String hostName) {
    final TypedQuery<MpackHostStateEntity> query = entityManagerProvider.get().createNamedQuery(
        "findInstallStateByHost", MpackHostStateEntity.class);
    query.setParameter("hostName", hostName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the install states for the given management pack.
   *
   * @param mpackId
   *          the ID of the mpack
   * @return all of the hosts in the cluster which have entries for the
   *         specified mpack.
   */
  @RequiresSession
  public List<MpackHostStateEntity> findByMpack(Long mpackId) {
    final TypedQuery<MpackHostStateEntity> query = entityManagerProvider.get().createNamedQuery(
        "findInstallStateByMpack", MpackHostStateEntity.class);
    query.setParameter("mpackId", mpackId);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve the installation state for a specific mpack on a host.
   *
   * @param mpackId
   *          the ID of the mpack
   * @param hostName
   *          FQDN of host
   * @return Return all of the mpack install states that match the criteria.
   */
  @RequiresSession
  public MpackHostStateEntity findByMpackAndHost(Long mpackId, String hostName) {
    final TypedQuery<MpackHostStateEntity> query = entityManagerProvider.get().createNamedQuery(
        "findInstallStateByMpackAndHost", MpackHostStateEntity.class);
    query.setParameter("mpackId", mpackId);
    query.setParameter("hostName", hostName);

    return daoUtils.selectOne(query);
  }

  /**
   * Retrieve all of the matching install states for any mpack on the given
   * host.
   *
   * @param mpackId
   *          the ID of the mpack
   * @param hostName
   *          FQDN of host
   * @return all of the hosts in the cluster which have entries for the
   *         specified mpack.
   */
  @RequiresSession
  public List<MpackHostStateEntity> findByHostAndInstallState(String hostName, MpackInstallState state) {
    final TypedQuery<MpackHostStateEntity> query = entityManagerProvider.get().createNamedQuery(
        "findInstallStateByStateAndHost", MpackHostStateEntity.class);
    query.setParameter("hostName", hostName);
    query.setParameter("mpackInstallState", state);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the matching install states for any mpack on the given
   * host.
   *
   * @param mpackId
   *          the ID of the mpack
   * @param hostName
   *          FQDN of host
   * @return all of the hosts in the cluster which have entries for the
   *         specified mpack.
   */
  @RequiresSession
  public List<MpackHostStateEntity> findByMpackAndInstallState(Long mpackId, MpackInstallState state) {
    final TypedQuery<MpackHostStateEntity> query = entityManagerProvider.get().createNamedQuery(
        "findInstallStateByMpackAndState", MpackHostStateEntity.class);
    query.setParameter("mpackId", mpackId);
    query.setParameter("mpackInstallState", state);

    return daoUtils.selectList(query);
  }


  /**
   * Removes all of the associated mpack host states for a given host.
   *
   * @param hostName
   *          the name of the host.
   */
  @Transactional
  public void removeByHostName(String hostName) {
    Collection<MpackHostStateEntity> mpackHostStates = findByHost(hostName);
    this.remove(mpackHostStates);
  }

}
