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

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.ComponentHostDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ComponentHostDesiredStateEntityPK;

import javax.persistence.EntityManager;

public class ComponentHostDesiredStateDAO {
  @Inject
  EntityManager entityManager;

  public ComponentHostDesiredStateEntity findByPK(ComponentHostDesiredStateEntityPK primaryKey) {
    return entityManager.find(ComponentHostDesiredStateEntity.class, primaryKey);
  }

  @Transactional
  public void create(ComponentHostDesiredStateEntity componentHostDesiredStateEntity) {
    entityManager.persist(componentHostDesiredStateEntity);
  }

  @Transactional
  public ComponentHostDesiredStateEntity merge(ComponentHostDesiredStateEntity componentHostDesiredStateEntity) {
    return entityManager.merge(componentHostDesiredStateEntity);
  }

  @Transactional
  public void remove(ComponentHostDesiredStateEntity componentHostDesiredStateEntity) {
    entityManager.remove(componentHostDesiredStateEntity);
  }

  @Transactional
  public void removeByPK(ComponentHostDesiredStateEntityPK primaryKey) {
    remove(findByPK(primaryKey));
  }

}
