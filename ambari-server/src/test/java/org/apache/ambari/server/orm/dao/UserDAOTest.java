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
import org.junit.Before;
import org.junit.Test;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import org.apache.ambari.server.orm.entities.RoleEntity;
import org.apache.ambari.server.orm.entities.UserEntity;

/**
 * BlueprintDAO unit tests.
 */
public class UserDAOTest {

  @Inject
  DaoUtils daoUtils;

  Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  EntityManager entityManager = createStrictMock(EntityManager.class);

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).atLeastOnce();
    replay(entityManagerProvider);
  }


  @Test
  public void testfindAllLocalUsersByRole() {
    UserEntity entity = new UserEntity();
    RoleEntity roleEntity = new RoleEntity();
    TypedQuery<UserEntity> query = createStrictMock(TypedQuery.class);

    // set expectations
    expect(entityManager.createQuery(eq("SELECT role.userEntities FROM RoleEntity role WHERE role = :roleEntity"), eq(UserEntity.class))).andReturn(query);
    roleEntity.setRoleName("admin");
    expect(query.setParameter("roleEntity", roleEntity)).andReturn(query);
    expect(query.getResultList()).andReturn(Collections.singletonList(entity));
    
    replay(entityManager, query);

    UserDAO dao = new UserDAO();
    dao.entityManagerProvider = entityManagerProvider;
    roleEntity.setRoleName("admin");
    
    List<UserEntity> results = dao.findAllLocalUsersByRole(roleEntity);

    assertEquals(1, results.size());
    assertSame(entity, results.get(0));

    verify(entityManagerProvider, entityManager, query);
  }

}
