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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.ActionEntity;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

public class ActionDefinitionDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Transactional
  public ActionEntity findByPK(String actionName) {
    return entityManagerProvider.get().find(ActionEntity.class, actionName);
  }

  @Transactional
  public List<ActionEntity> findAll() {
    TypedQuery<ActionEntity> query = entityManagerProvider.get().createNamedQuery("allActions",
        ActionEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @Transactional
  public void create(ActionEntity actionDefinition) {
    entityManagerProvider.get().persist(actionDefinition);
  }

  @Transactional
  public ActionEntity merge(ActionEntity actionDefinition) {
    return entityManagerProvider.get().merge(actionDefinition);
  }

  @Transactional
  public void remove(ActionEntity actionDefinition) {
    entityManagerProvider.get().remove(merge(actionDefinition));
  }

  @Transactional
  public void removeByPK(String actionName) {
    remove(findByPK(actionName));
  }
}
