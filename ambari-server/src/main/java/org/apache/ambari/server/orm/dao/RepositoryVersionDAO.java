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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.TypedQuery;

import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.VersionUtils;

import com.google.inject.Singleton;

/**
 * DAO for repository versions.
 *
 */
@Singleton
public class RepositoryVersionDAO extends CrudDAO<RepositoryVersionEntity, Long> {
  /**
   * Constructor.
   */
  public RepositoryVersionDAO() {
    super(RepositoryVersionEntity.class);
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
   * Retrieves repository version by stack.
   *
   * @param stackId
   *          stackId
   * @param version
   *          version
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public RepositoryVersionEntity findByStackAndVersion(StackId stackId,
      String version) {
    return findByStackAndVersion(stackId.getStackName(),
        stackId.getStackVersion(), version);
  }

  /**
   * Retrieves repository version by stack.
   *
   * @param stackEntity Stack entity
   * @param version version
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public RepositoryVersionEntity findByStackAndVersion(StackEntity stackEntity,
      String version) {
    return findByStackAndVersion(stackEntity.getStackName(),
        stackEntity.getStackVersion(), version);
  }

  /**
   * Retrieves repository version by stack.
   *
   * @param stackName
   *          stack name
   * @param stackVersion
   *          stack version
   * @param version
   *          version
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  private RepositoryVersionEntity findByStackAndVersion(String stackName,
      String stackVersion, String version) {
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery(
        "repositoryVersionByStackVersion", RepositoryVersionEntity.class);
    query.setParameter("stackName", stackName);
    query.setParameter("stackVersion", stackVersion);
    query.setParameter("version", version);
    return daoUtils.selectSingle(query);
  }

  /**
   * Retrieves repository version by stack.
   *
   * @param version version
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public List<RepositoryVersionEntity> findByVersion(String version) {
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery("repositoryVersionByVersion", RepositoryVersionEntity.class);
    query.setParameter("version", version);
    return daoUtils.selectList(query);
  }

  /**
   * Retrieves repository version by stack.
   *
   * @param stackId stack id
   *          stack with major version (like HDP-2.2)
   * @return null if there is no suitable repository version
   */
  @RequiresSession
  public List<RepositoryVersionEntity> findByStack(StackId stackId) {
    final TypedQuery<RepositoryVersionEntity> query = entityManagerProvider.get().createNamedQuery("repositoryVersionByStack", RepositoryVersionEntity.class);
    query.setParameter("stackName", stackId.getStackName());
    query.setParameter("stackVersion", stackId.getStackVersion());
    return daoUtils.selectList(query);
  }

  /**
   * Validates and creates an object.
   * @param stackEntity Stack entity
   * @param version Stack version, e.g., 2.2 or 2.2.0.1-885
   * @param displayName Unique display name
   * @param upgradePack Optional upgrade pack, e.g, upgrade-2.2
   * @param operatingSystems JSON structure of repository URLs for each OS
   * @return Returns the object created if successful, and throws an exception otherwise.
   * @throws AmbariException
   */
  @Transactional
  public RepositoryVersionEntity create(StackEntity stackEntity,
      String version, String displayName, String upgradePack,
      String operatingSystems) throws AmbariException {

    if (stackEntity == null || version == null || version.isEmpty()
        || displayName == null || displayName.isEmpty()) {
      throw new AmbariException("At least one of the required properties is null or empty");
    }

    RepositoryVersionEntity existingByDisplayName = findByDisplayName(displayName);

    if (existingByDisplayName != null) {
      throw new AmbariException("Repository version with display name '" + displayName + "' already exists");
    }

    RepositoryVersionEntity existingByStackAndVersion = findByStackAndVersion(
        stackEntity, version);

    if (existingByStackAndVersion != null) {
      throw new AmbariException("Repository version for stack " + stackEntity
          + " and version " + version + " already exists");
    }

    RepositoryVersionEntity newEntity = new RepositoryVersionEntity(
        stackEntity, version, displayName, upgradePack, operatingSystems);
    this.create(newEntity);
    return newEntity;
  }

  /**
   * Finds the repository version, making sure if there is more than one match
   * (unlikely) that the latest stack is chosen.
   * @param version the version to find
   * @return the matching repo version entity
   */
  @RequiresSession
  public RepositoryVersionEntity findMaxByVersion(String version) {
    List<RepositoryVersionEntity> list = findByVersion(version);
    if (null == list || 0 == list.size()) {
      return null;
    } else if (1 == list.size()) {
      return list.get(0);
    } else {
      Collections.sort(list, new Comparator<RepositoryVersionEntity>() {
        @Override
        public int compare(RepositoryVersionEntity o1, RepositoryVersionEntity o2) {
          return VersionUtils.compareVersions(o1.getVersion(), o2.getVersion());
        }
      });

      Collections.reverse(list);

      return list.get(0);
    }

  }


}
