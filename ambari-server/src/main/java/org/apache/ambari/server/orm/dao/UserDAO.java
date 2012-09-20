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
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.UserEntity;

import javax.persistence.EntityManager;

public class UserDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;

  public UserEntity findByName(String userName) {
    return entityManagerProvider.get().find(UserEntity.class, userName);
  }

  public void create(UserEntity userName) {
    entityManagerProvider.get().persist(userName);
  }

  @Transactional
  public UserEntity merge(UserEntity userName) {
    return entityManagerProvider.get().merge(userName);
  }

  @Transactional
  public void remove(UserEntity userName) {
    entityManagerProvider.get().remove(userName);
  }

  @Transactional
  public void removeByName(String userName) {
    remove(findByName(userName));
  }

}
