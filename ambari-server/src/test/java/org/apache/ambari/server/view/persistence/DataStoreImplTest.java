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

package org.apache.ambari.server.view.persistence;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.configuration.EntityConfig;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.easymock.Capture;
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.jpa.dynamic.JPADynamicHelper;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.tools.schemaframework.SchemaManager;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * DataStoreImpl tests.
 */
public class DataStoreImplTest {
  private final static String xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "    </instance>\n" +
      "    <persistence>\n" +
      "      <entity>\n" +
      "        <class>org.apache.ambari.server.view.persistence.DataStoreImplTest$TestEntity</class>\n" +
      "        <id-property>id</id-property>\n" +
      "      </entity>\n" +
      "    </persistence>" +
      "</view>";

  @Test
  public void testStore_create() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);

    // set expectations
    Capture<DynamicType> typeCapture = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    Capture<Class> entityClassCapture = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture), eq(99))).andReturn(null);
    Capture<DynamicEntity> entityCapture = new Capture<DynamicEntity>();
    entityManager.persist(capture(entityCapture));
    entityManager.close();

    transaction.begin();
    transaction.commit();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    dataStore.store(new TestEntity(99, "foo"));

    Assert.assertEquals(entityClassCapture.getValue(), typeCapture.getValue().getJavaClass());
    Assert.assertEquals(99, entityCapture.getValue().get("id"));
    Assert.assertEquals("foo", entityCapture.getValue().get("name"));

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager);
  }

  @Test
  public void testStore_update() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);
    DynamicEntity dynamicEntity = createMock(DynamicEntity.class);

    // set expectations
    Capture<DynamicType> typeCapture = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    Capture<Class> entityClassCapture = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture), eq(99))).andReturn(dynamicEntity);
    entityManager.close();

    expect(dynamicEntity.set("id", 99)).andReturn(dynamicEntity);
    expect(dynamicEntity.set("name", "foo")).andReturn(dynamicEntity);

    transaction.begin();
    transaction.commit();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    dataStore.store(new TestEntity(99, "foo"));

    Assert.assertEquals(entityClassCapture.getValue(), typeCapture.getValue().getJavaClass());

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity);
  }

  @Test
  public void testRemove() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);
    DynamicEntity dynamicEntity = createMock(DynamicEntity.class);

    // set expectations
    Capture<DynamicType> typeCapture = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();
    Capture<Class> entityClassCapture = new Capture<Class>();
    expect(entityManager.getReference(capture(entityClassCapture), eq(99))).andReturn(dynamicEntity);
    entityManager.remove(dynamicEntity);
    entityManager.close();

    transaction.begin();
    transaction.commit();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    dataStore.remove(new TestEntity(99, "foo"));

    Assert.assertEquals(entityClassCapture.getValue(), typeCapture.getValue().getJavaClass());

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity);
  }

  @Test
  public void testFind() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    DynamicEntity dynamicEntity = createMock(DynamicEntity.class);

    // set expectations
    Capture<DynamicType> typeCapture = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    Capture<Class> entityClassCapture = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture), eq(99))).andReturn(dynamicEntity);
    entityManager.close();

    expect(dynamicEntity.get("id")).andReturn(99);
    expect(dynamicEntity.get("name")).andReturn("foo");

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, dynamicEntity, schemaManager);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    TestEntity entity = dataStore.find(TestEntity.class, 99);

    Assert.assertEquals(entityClassCapture.getValue(), typeCapture.getValue().getJavaClass());
    Assert.assertEquals(99, entity.getId());
    Assert.assertEquals("foo", entity.getName());

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, dynamicEntity, schemaManager);
  }

  @Test
  public void testFindAll() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    DynamicEntity dynamicEntity = createMock(DynamicEntity.class);
    Query query = createMock(Query.class);

    // set expectations
    Capture<DynamicType> typeCapture = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.createQuery(
        "SELECT e FROM DataStoreImplTest$TestEntity1 e WHERE e.id=99")).andReturn(query);
    entityManager.close();

    expect(query.getResultList()).andReturn(Collections.singletonList(dynamicEntity));

    expect(dynamicEntity.get("id")).andReturn(99);
    expect(dynamicEntity.get("name")).andReturn("foo");

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, dynamicEntity, query, schemaManager);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    Collection<TestEntity> entities = dataStore.findAll(TestEntity.class, "id=99");

    Assert.assertEquals(1, entities.size());

    TestEntity entity = entities.iterator().next();

    Assert.assertEquals(99, entity.getId());
    Assert.assertEquals("foo", entity.getName());

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, dynamicEntity, query, schemaManager);
  }

  @Test
  public void testFindAll_multiple() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    DynamicEntity dynamicEntity1 = createMock(DynamicEntity.class);
    DynamicEntity dynamicEntity2 = createMock(DynamicEntity.class);
    DynamicEntity dynamicEntity3 = createMock(DynamicEntity.class);
    Query query = createMock(Query.class);

    // set expectations
    Capture<DynamicType> typeCapture = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.createQuery(
        "SELECT e FROM DataStoreImplTest$TestEntity1 e WHERE e.name='foo'")).andReturn(query);
    entityManager.close();

    List<DynamicEntity> entityList = new LinkedList<DynamicEntity>();
    entityList.add(dynamicEntity1);
    entityList.add(dynamicEntity2);
    entityList.add(dynamicEntity3);

    expect(query.getResultList()).andReturn(entityList);

    expect(dynamicEntity1.get("id")).andReturn(99);
    expect(dynamicEntity1.get("name")).andReturn("foo");

    expect(dynamicEntity2.get("id")).andReturn(100);
    expect(dynamicEntity2.get("name")).andReturn("foo");

    expect(dynamicEntity3.get("id")).andReturn(101);
    expect(dynamicEntity3.get("name")).andReturn("foo");

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper,
        dynamicEntity1, dynamicEntity2, dynamicEntity3, query, schemaManager);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    Collection<TestEntity> entities = dataStore.findAll(TestEntity.class, "name='foo'");

    Assert.assertEquals(3, entities.size());

    for (TestEntity entity : entities) {
      Assert.assertEquals("foo", entity.getName());
    }

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper,
        dynamicEntity1, dynamicEntity2, dynamicEntity3, query, schemaManager);
  }

  private DataStoreImpl getDataStore(EntityManagerFactory entityManagerFactory,
                                     JPADynamicHelper jpaDynamicHelper,
                                     DynamicClassLoader classLoader,
                                     SchemaManager schemaManager)
      throws Exception {
    ViewConfig viewConfig = ViewConfigTest.getConfig(xml);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity(viewConfig);

    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity(viewDefinition, instanceConfig);

    setPersistenceEntities(viewInstanceEntity);

    Injector injector = Guice.createInjector(
        new TestModule(viewInstanceEntity, entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager));
    return injector.getInstance(DataStoreImpl.class);
  }


  // TODO : move to ViewEntityEntity test.
  private static void setPersistenceEntities(ViewInstanceEntity viewInstanceDefinition) {
    ViewEntity viewDefinition = viewInstanceDefinition.getViewEntity();
    Collection<ViewEntityEntity> entities = new HashSet<ViewEntityEntity>();

    ViewConfig viewConfig = viewDefinition.getConfiguration();
    for (EntityConfig entityConfiguration : viewConfig.getPersistence().getEntities()) {
      ViewEntityEntity viewEntityEntity = new ViewEntityEntity();

      viewEntityEntity.setId(1L);
      viewEntityEntity.setViewName(viewDefinition.getName());
      viewEntityEntity.setViewInstanceName(viewInstanceDefinition.getName());
      viewEntityEntity.setClassName(entityConfiguration.getClassName());
      viewEntityEntity.setIdProperty(entityConfiguration.getIdProperty());
      viewEntityEntity.setViewInstance(viewInstanceDefinition);

      entities.add(viewEntityEntity);
    }
    viewInstanceDefinition.setEntities(entities);
  }


  public static class TestEntity {

    public TestEntity() {
    }

    public TestEntity(int id, String name) {
      this.id = id;
      this.name = name;
    }

    int id;
    String name;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private static class TestModule implements Module, SchemaManagerFactory {
    private final ViewInstanceEntity viewInstanceEntity;
    private final EntityManagerFactory entityManagerFactory;
    private final JPADynamicHelper jpaDynamicHelper;
    private final DynamicClassLoader classLoader;
    private final SchemaManager schemaManager;

    private TestModule(ViewInstanceEntity viewInstanceEntity, EntityManagerFactory entityManagerFactory,
                       JPADynamicHelper jpaDynamicHelper, DynamicClassLoader classLoader,
                       SchemaManager schemaManager) {
      this.viewInstanceEntity = viewInstanceEntity;
      this.entityManagerFactory = entityManagerFactory;
      this.jpaDynamicHelper = jpaDynamicHelper;
      this.classLoader = classLoader;
      this.schemaManager = schemaManager;
    }

    @Override
    public void configure(Binder binder) {
      binder.bind(ViewInstanceEntity.class).toInstance(viewInstanceEntity);
      binder.bind(EntityManagerFactory.class).toInstance(entityManagerFactory);
      binder.bind(JPADynamicHelper.class).toInstance(jpaDynamicHelper);
      binder.bind(DynamicClassLoader.class).toInstance(classLoader);
      binder.bind(SchemaManagerFactory.class).toInstance(this);
    }

    @Override
    public SchemaManager getSchemaManager(DatabaseSession session) {
      return schemaManager;
    }
  }
}
