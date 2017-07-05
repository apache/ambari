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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

/**
 * DAO dealing with ambari configuration related JPA operations.
 * Operations delegate to the JPA provider implementation of CRUD operations.
 */

@Singleton
public class AmbariConfigurationDAO extends CrudDAO<AmbariConfigurationEntity, Long> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationDAO.class);

  @Inject
  public AmbariConfigurationDAO() {
    super(AmbariConfigurationEntity.class);
  }

  @Transactional
  public void create(AmbariConfigurationEntity entity) {
    // make  sure only one LDAP config entry exists
    if ("ldap-configuration".equals(entity.getConfigurationBaseEntity().getType())) {
      AmbariConfigurationEntity ldapConfigEntity = getLdapConfiguration();
      if (ldapConfigEntity != null) {
        LOGGER.error("Only one LDAP configuration entry can exist!");
        throw new EntityExistsException("LDAP configuration entity already exists!");
      }
    }
    super.create(entity);
  }


  @Transactional
  public void update(AmbariConfigurationEntity entity) {
    if (entity.getId() == null || findByPK(entity.getId()) == null) {
      String msg = String.format("The entity with id [ %s ] is not found", entity.getId());
      LOGGER.debug(msg);
      throw new EntityNotFoundException(msg);
    }

    // updating the existing entity
    super.merge(entity);
    entityManagerProvider.get().flush();
  }

  /**
   * Returns the LDAP configuration from the database.
   *
   * @return the configuration entity
   */
  @Transactional
  public AmbariConfigurationEntity getLdapConfiguration() {
    LOGGER.info("Looking up the LDAP configuration ....");
    AmbariConfigurationEntity ldapConfigEntity = null;

    TypedQuery<AmbariConfigurationEntity> query = entityManagerProvider.get().createNamedQuery(
      "AmbariConfigurationEntity.findByType", AmbariConfigurationEntity.class);
    query.setParameter("typeName", "ldap-configuration");

    ldapConfigEntity = daoUtils.selectSingle(query);
    LOGGER.info("Returned entity: {} ", ldapConfigEntity);
    return ldapConfigEntity;
  }
}
