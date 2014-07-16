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

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Tests {@link AlertDefinitionDAO} for interacting with
 * {@link AlertDefinitionEntity}.
 */
@SuppressWarnings("unchecked")
public class AlertDefinitionDAOTest {

  AlertDefinitionDAO dao;
  Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  EntityManager entityManager = createStrictMock(EntityManager.class);

  private Injector injector;

  /**
   * 
   */
  @Before
  public void init() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).atLeastOnce();
    replay(entityManagerProvider);

    dao = new AlertDefinitionDAO();
    injector.injectMembers(dao);

    dao.entityManagerProvider = entityManagerProvider;
  }

  /**
   * 
   */
  @Test
  public void testFindByName() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    TypedQuery<AlertDefinitionEntity> query = createStrictMock(TypedQuery.class);
    
    expect(query.setParameter("definitionName", "alert-definition-1")).andReturn(
        query);

    expect(query.getSingleResult()).andReturn(entity);

    expect(
        entityManager.createNamedQuery(eq("AlertDefinitionEntity.findByName"),
            eq(AlertDefinitionEntity.class))).andReturn(query);

    replay(query, entityManager);

    AlertDefinitionEntity result = dao.findByName("alert-definition-1");

    assertSame(result, entity);
    verify(entityManagerProvider, entityManager);
  }

  /**
   * 
   */
  @Test
  public void testFindAll() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    TypedQuery<AlertDefinitionEntity> query = createStrictMock(TypedQuery.class);

    expect(query.getResultList()).andReturn(Collections.singletonList(entity));

    expect(
        entityManager.createNamedQuery(eq("AlertDefinitionEntity.findAll"),
            eq(AlertDefinitionEntity.class))).andReturn(query);

    replay(query, entityManager);

    List<AlertDefinitionEntity> entities = dao.findAll();

    assertSame(1, entities.size());
    assertSame(entity, entities.get(0));
    verify(entityManagerProvider, entityManager);
  }

  /**
   * 
   */
  @Test
  public void findById() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();

    expect(entityManager.find(eq(AlertDefinitionEntity.class), eq(12345L))).andReturn(
        entity);

    replay(entityManager);

    AlertDefinitionEntity result = dao.findById(12345L);

    assertSame(result, entity);
    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testRefresh() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();

    // set expectations
    entityManager.refresh(eq(entity));
    replay(entityManager);

    dao.entityManagerProvider = entityManagerProvider;
    dao.refresh(entity);

    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testCreate() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();

    // set expectations
    entityManager.persist(eq(entity));
    replay(entityManager);

    dao.entityManagerProvider = entityManagerProvider;
    dao.create(entity);

    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testMerge() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    AlertDefinitionEntity entity2 = new AlertDefinitionEntity();

    // set expectations
    expect(entityManager.merge(eq(entity))).andReturn(entity2);
    replay(entityManager);

    dao.entityManagerProvider = entityManagerProvider;
    assertSame(entity2, dao.merge(entity));

    verify(entityManagerProvider, entityManager);
  }

  @Test
  public void testRemove() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    AlertDefinitionEntity entity2 = new AlertDefinitionEntity();

    // set expectations
    expect(entityManager.merge(eq(entity))).andReturn(entity2);
    entityManager.remove(eq(entity2));
    replay(entityManager);

    dao.entityManagerProvider = entityManagerProvider;
    dao.remove(entity);

    verify(entityManagerProvider, entityManager);
  }
}
