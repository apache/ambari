/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

/**
 * DAO dealing with ambari configuration related JPA operations.
 */

@Singleton
// todo extend CrudDao (amend crud dao to handle NPEs)
public class AmbariConfigurationDAO {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationDAO.class);

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils daoUtils;

  public AmbariConfigurationEntity findByid(Long id) {
    return entityManagerProvider.get().find(AmbariConfigurationEntity.class, id);
  }

  @RequiresSession
  @Transactional
  public void persist(AmbariConfigurationEntity entity) {
    LOGGER.debug("Persisting ambari configuration: {}", entity);
    entityManagerProvider.get().persist(entity);
  }

  @RequiresSession
  public List<AmbariConfigurationEntity> findAll() {
    TypedQuery<AmbariConfigurationEntity> query = entityManagerProvider.get().createNamedQuery(
      "AmbariConfigurationEntity.findAll", AmbariConfigurationEntity.class);
    return daoUtils.selectList(query);
  }


  @RequiresSession
  @Transactional
  public void deleteById(Long ambariConfigurationId) {

    if (ambariConfigurationId == null) {
      throw new IllegalArgumentException("No Ambari Configuration id provided.");
    }

    LOGGER.debug("Removing Ambari Configuration with id :{}", ambariConfigurationId);

    AmbariConfigurationEntity ambariConfigurationEntity = findByid(ambariConfigurationId);
    if (ambariConfigurationEntity == null) {
      String msg = String.format("No Ambari Configuration found with id: %s", ambariConfigurationId);
      LOGGER.debug(msg);
      throw new IllegalStateException(msg);
    }

    entityManagerProvider.get().remove(ambariConfigurationEntity);
    LOGGER.debug("Ambari Configuration with id: {}", ambariConfigurationId);
  }


}
