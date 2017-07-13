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

import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;

import com.google.inject.persist.Transactional;

/**
 * DAO dealing with ambari configuration related JPA operations.
 * Operations delegate to the JPA provider implementation of CRUD operations.
 */

@Singleton
public class AmbariConfigurationDAO extends CrudDAO<AmbariConfigurationEntity, Long> {

  @Inject
  public AmbariConfigurationDAO() {
    super(AmbariConfigurationEntity.class);
  }

  @Transactional
  public void create(AmbariConfigurationEntity entity) {
    super.create(entity);
  }
}
