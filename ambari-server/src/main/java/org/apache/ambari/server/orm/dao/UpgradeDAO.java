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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.apache.ambari.server.orm.entities.UpgradeEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Manages the UpgradeEntity and UpgradeItemEntity classes
 */
@Singleton
public class UpgradeDAO {

  @Inject
  private Provider<EntityManager> m_entityManagerProvider;

  // !!! TODO tie into JPA framework.  For now, just return skeleton classes
  private AtomicLong m_idGen = new AtomicLong(1L);
  private List<UpgradeEntity> m_entities = new ArrayList<UpgradeEntity>();

  /**
   * @param clusterId the cluster id
   * @return the list of upgrades initiated for the cluster
   */
  public List<UpgradeEntity> findUpgrades(long clusterId) {
    return m_entities;
  }

  /**
   * Finds a specific upgrade
   * @param upgradeId the id
   * @return the entity, or {@code null} if not found
   */
  public UpgradeEntity findUpgrade(long upgradeId) {
    for (UpgradeEntity ue : m_entities) {
      if (ue.getId().longValue() == upgradeId) {
        return ue;
      }
    }
    return null;
  }

  /**
   * Creates the upgrade entity in the database
   */
  @Transactional
  public void create(UpgradeEntity entity) {
    entity.setId(Long.valueOf(m_idGen.getAndIncrement()));
    m_entities.add(entity);
  }


}
