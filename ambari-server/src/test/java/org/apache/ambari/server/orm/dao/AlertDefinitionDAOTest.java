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
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;

/**
 * Tests {@link AlertDefinitionDAO} for interacting with
 * {@link AlertDefinitionEntity}.
 */
@SuppressWarnings("unchecked")
public class AlertDefinitionDAOTest {

  AlertDefinitionDAO realDAO;
  AlertDefinitionDAO mockDAO;
  Provider<EntityManager> mockEntityManagerProvider = createStrictMock(Provider.class);
  EntityManager entityManager = createStrictMock(EntityManager.class);

  private static Injector injector;
  private static Long clusterId;

  @BeforeClass
  public static void beforeClass() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    clusterId = injector.getInstance(OrmTestHelper.class).createCluster();

    for (int i = 0; i < 8; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("HDFS");
      definition.setComponentName(null);
      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60L);
      definition.setScope("SERVICE");
      definition.setSource("Source " + i);
      definition.setSourceType("SCRIPT");
      alertDefinitionDAO.create(definition);
    }
  }

  @AfterClass
  public static void afterClass() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  /**
   * 
   */
  @Before
  public void init() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    reset(mockEntityManagerProvider);
    expect(mockEntityManagerProvider.get()).andReturn(entityManager).atLeastOnce();
    replay(mockEntityManagerProvider);

    realDAO = new AlertDefinitionDAO();
    mockDAO = new AlertDefinitionDAO();
    injector.injectMembers(realDAO);
    injector.injectMembers(mockDAO);

    mockDAO.entityManagerProvider = mockEntityManagerProvider;
  }

  /**
   * 
   */
  @Test
  public void testFindByName() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    TypedQuery<AlertDefinitionEntity> query = createStrictMock(TypedQuery.class);

    expect(query.setParameter("clusterId", 12345L)).andReturn(query);

    expect(query.setParameter("definitionName", "alert-definition-1")).andReturn(
        query);

    expect(query.getSingleResult()).andReturn(entity);

    expect(
        entityManager.createNamedQuery(eq("AlertDefinitionEntity.findByName"),
            eq(AlertDefinitionEntity.class))).andReturn(query);

    replay(query, entityManager);

    AlertDefinitionEntity result = mockDAO.findByName(12345L,
        "alert-definition-1");

    assertSame(result, entity);
    verify(mockEntityManagerProvider, entityManager);

    List<AlertDefinitionEntity> definitions = realDAO.findAll();
    Assert.assertNotNull(definitions);
    AlertDefinitionEntity definition = definitions.get(2);
    AlertDefinitionEntity retrieved = realDAO.findByName(
        definition.getClusterId(), definition.getDefinitionName());

    Assert.assertEquals(definition, retrieved);
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

    List<AlertDefinitionEntity> entities = mockDAO.findAll();

    assertSame(1, entities.size());
    assertSame(entity, entities.get(0));
    verify(mockEntityManagerProvider, entityManager);

    List<AlertDefinitionEntity> definitions = realDAO.findAll();
    Assert.assertNotNull(definitions);
    Assert.assertEquals(8, definitions.size());
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

    AlertDefinitionEntity result = mockDAO.findById(12345L);

    assertSame(result, entity);
    verify(mockEntityManagerProvider, entityManager);

    List<AlertDefinitionEntity> definitions = realDAO.findAll();
    Assert.assertNotNull(definitions);
    AlertDefinitionEntity definition = definitions.get(2);
    AlertDefinitionEntity retrieved = realDAO.findById(definition.getDefinitionId());

    Assert.assertEquals(definition, retrieved);
  }

  @Test
  public void testRefresh() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();

    // set expectations
    entityManager.refresh(eq(entity));
    replay(entityManager);

    mockDAO.entityManagerProvider = mockEntityManagerProvider;
    mockDAO.refresh(entity);

    verify(mockEntityManagerProvider, entityManager);
  }

  @Test
  public void testCreate() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();

    // set expectations
    entityManager.persist(eq(entity));
    replay(entityManager);

    mockDAO.entityManagerProvider = mockEntityManagerProvider;
    mockDAO.create(entity);

    verify(mockEntityManagerProvider, entityManager);
  }

  @Test
  public void testMerge() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    AlertDefinitionEntity entity2 = new AlertDefinitionEntity();

    // set expectations
    expect(entityManager.merge(eq(entity))).andReturn(entity2);
    replay(entityManager);

    mockDAO.entityManagerProvider = mockEntityManagerProvider;
    assertSame(entity2, mockDAO.merge(entity));

    verify(mockEntityManagerProvider, entityManager);
  }

  @Test
  public void testRemove() {
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    AlertDefinitionEntity entity2 = new AlertDefinitionEntity();

    // set expectations
    expect(entityManager.merge(eq(entity))).andReturn(entity2);
    entityManager.remove(eq(entity2));
    replay(entityManager);

    mockDAO.entityManagerProvider = mockEntityManagerProvider;
    mockDAO.remove(entity);

    verify(mockEntityManagerProvider, entityManager);
  }
}
