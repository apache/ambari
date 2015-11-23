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
import org.apache.ambari.view.PersistenceException;
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
      "      <entity>\n" +
      "        <class>org.apache.ambari.server.view.persistence.DataStoreImplTest$TestSubEntity</class>\n" +
      "        <id-property>name</id-property>\n" +
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
    Capture<DynamicType> typeCapture2 = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    Capture<Class> entityClassCapture = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture), eq("bar"))).andReturn(null);

    Capture<Class> entityClassCapture2 = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture2), eq(99))).andReturn(null);

    Capture<DynamicEntity> entityCapture = new Capture<DynamicEntity>();
    entityManager.persist(capture(entityCapture));

    Capture<DynamicEntity> entityCapture2 = new Capture<DynamicEntity>();
    entityManager.persist(capture(entityCapture2));

    entityManager.close();

    transaction.begin();
    transaction.commit();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    dataStore.store(new TestEntity(99, "foo", new TestSubEntity("bar")));

    Assert.assertEquals(entityClassCapture.getValue(), typeCapture.getValue().getJavaClass());
    Assert.assertEquals(entityClassCapture2.getValue(), typeCapture2.getValue().getJavaClass());

    Assert.assertEquals("bar", entityCapture.getValue().get("DS_name"));

    Assert.assertEquals(99, entityCapture2.getValue().get("DS_id"));
    Assert.assertEquals("foo", entityCapture2.getValue().get("DS_name"));

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager);
  }

  @Test
  public void testStore_create_longStringValue() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);

    // set expectations
    Capture<DynamicType> typeCapture = new Capture<DynamicType>();
    Capture<DynamicType> typeCapture2 = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    Capture<Class> entityClassCapture2 = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture2), eq(99))).andReturn(null);

    entityManager.close();

    transaction.begin();
    expect(transaction.isActive()).andReturn(true);
    transaction.rollback();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 5000; ++i) {
      sb.append("A");
    }
    String longString = sb.toString();

    try {
      dataStore.store(new TestEntity(99, longString, new TestSubEntity("bar")));
      Assert.fail("Expected PersistenceException.");
    } catch (PersistenceException e) {
      // expected
    }
    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager);
  }

  @Test
  public void testStore_create_largeEntity() throws Exception {
    DynamicClassLoader classLoader = new DynamicClassLoader(DataStoreImplTest.class.getClassLoader());

    // create mocks
    EntityManagerFactory entityManagerFactory = createMock(EntityManagerFactory.class);
    EntityManager entityManager = createMock(EntityManager.class);
    JPADynamicHelper jpaDynamicHelper = createNiceMock(JPADynamicHelper.class);
    SchemaManager schemaManager = createNiceMock(SchemaManager.class);
    EntityTransaction transaction = createMock(EntityTransaction.class);

    // set expectations
    Capture<DynamicType> typeCapture = new Capture<DynamicType>();
    Capture<DynamicType> typeCapture2 = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    entityManager.close();

    transaction.begin();
    expect(transaction.isActive()).andReturn(true);
    transaction.rollback();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    try {
      dataStore.store(new TestLargeEntity(99));
      Assert.fail("Expected PersistenceException.");
    } catch (PersistenceException e) {
      // expected
    }
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
    Capture<DynamicType> typeCapture2 = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    Capture<Class> entityClassCapture = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture), eq("bar"))).andReturn(null);

    Capture<Class> entityClassCapture2 = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture2), eq(99))).andReturn(dynamicEntity);

    Capture<DynamicEntity> entityCapture = new Capture<DynamicEntity>();
    entityManager.persist(capture(entityCapture));

    entityManager.close();

    expect(dynamicEntity.set("DS_id", 99)).andReturn(dynamicEntity);
    expect(dynamicEntity.set("DS_name", "foo")).andReturn(dynamicEntity);

    Capture<DynamicEntity> subEntityCapture = new Capture<DynamicEntity>();
    expect(dynamicEntity.set(eq("DS_subEntity"), capture(subEntityCapture))).andReturn(dynamicEntity);

    transaction.begin();
    transaction.commit();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    dataStore.store(new TestEntity(99, "foo", new TestSubEntity("bar")));

    Assert.assertEquals(entityClassCapture.getValue(), typeCapture.getValue().getJavaClass());
    Assert.assertEquals(entityClassCapture2.getValue(), typeCapture2.getValue().getJavaClass());

    // verify mocks
    verify(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity);
  }

  @Test
  public void testStore_update_longStringValue() throws Exception {
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
    Capture<DynamicType> typeCapture2 = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.getTransaction()).andReturn(transaction).anyTimes();

    Capture<Class> entityClassCapture2 = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture2), eq(99))).andReturn(dynamicEntity);

    entityManager.close();

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 5000; ++i) {
      sb.append("A");
    }
    String longString = sb.toString();

    expect(dynamicEntity.set("DS_id", 99)).andReturn(dynamicEntity).times(0, 1);

    transaction.begin();
    expect(transaction.isActive()).andReturn(true).anyTimes();
    transaction.rollback();

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, transaction, schemaManager, dynamicEntity);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    try {
      dataStore.store(new TestEntity(99, longString, new TestSubEntity("bar")));
      Assert.fail();
    } catch (PersistenceException e) {
      // expected
    }

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
    Capture<DynamicType> typeCapture2 = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

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

    dataStore.remove(new TestEntity(99, "foo", new TestSubEntity("bar")));

    Assert.assertEquals(entityClassCapture.getValue(), typeCapture2.getValue().getJavaClass());

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
    Capture<DynamicType> typeCapture2 = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    Capture<Class> entityClassCapture = new Capture<Class>();
    expect(entityManager.find(capture(entityClassCapture), eq(99))).andReturn(dynamicEntity);
    entityManager.close();

    expect(dynamicEntity.get("DS_id")).andReturn(99);
    expect(dynamicEntity.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity = new TestSubEntity("bar");
    expect(dynamicEntity.get("DS_subEntity")).andReturn(subEntity);

    // replay mocks
    replay(entityManagerFactory, entityManager, jpaDynamicHelper, dynamicEntity, schemaManager);

    DataStoreImpl dataStore = getDataStore(entityManagerFactory, jpaDynamicHelper, classLoader, schemaManager);

    TestEntity entity = dataStore.find(TestEntity.class, 99);

    Assert.assertEquals(entityClassCapture.getValue(), typeCapture2.getValue().getJavaClass());
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
    Capture<DynamicType> typeCapture2 = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.createQuery(
        "SELECT e FROM DS_DataStoreImplTest$TestEntity_1 e WHERE e.DS_id=99")).andReturn(query);
    entityManager.close();

    expect(query.getResultList()).andReturn(Collections.singletonList(dynamicEntity));

    expect(dynamicEntity.get("DS_id")).andReturn(99);
    expect(dynamicEntity.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity = new TestSubEntity("bar");
    expect(dynamicEntity.get("DS_subEntity")).andReturn(subEntity);

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
    Capture<DynamicType> typeCapture2 = new Capture<DynamicType>();
    jpaDynamicHelper.addTypes(eq(true), eq(true), capture(typeCapture), capture(typeCapture2));

    expect(entityManagerFactory.createEntityManager()).andReturn(entityManager);
    expect(entityManager.createQuery(
        "SELECT e FROM DS_DataStoreImplTest$TestEntity_1 e WHERE e.DS_name='foo'")).andReturn(query);
    entityManager.close();

    List<DynamicEntity> entityList = new LinkedList<DynamicEntity>();
    entityList.add(dynamicEntity1);
    entityList.add(dynamicEntity2);
    entityList.add(dynamicEntity3);

    expect(query.getResultList()).andReturn(entityList);

    expect(dynamicEntity1.get("DS_id")).andReturn(99);
    expect(dynamicEntity1.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity1 = new TestSubEntity("bar");
    expect(dynamicEntity1.get("DS_subEntity")).andReturn(subEntity1);

    expect(dynamicEntity2.get("DS_id")).andReturn(100);
    expect(dynamicEntity2.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity2 = new TestSubEntity("bar");
    expect(dynamicEntity2.get("DS_subEntity")).andReturn(subEntity2);

    expect(dynamicEntity3.get("DS_id")).andReturn(101);
    expect(dynamicEntity3.get("DS_name")).andReturn("foo");
    TestSubEntity subEntity3 = new TestSubEntity("bar");
    expect(dynamicEntity3.get("DS_subEntity")).andReturn(subEntity3);

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

    public TestEntity(int id, String name, TestSubEntity subEntity) {
      this.id = id;
      this.name = name;
      this.subEntity = subEntity;
    }

    int id;
    String name;
    TestSubEntity subEntity;

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

    public TestSubEntity getSubEntity() {
      return subEntity;
    }

    public void setSubEntity(TestSubEntity subEntity) {
      this.subEntity = subEntity;
    }
  }

  public static class TestSubEntity {

    public TestSubEntity() {
    }

    public TestSubEntity(String name) {
      this.name = name;
    }

    String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class TestLargeEntity {

    public TestLargeEntity() {
    }

    public TestLargeEntity(int id) {
      this.id = id;
    }

    int id;
    String f1;
    String f2;
    String f3;
    String f4;
    String f5;
    String f6;
    String f7;
    String f8;
    String f9;
    String f10;
    String f11;
    String f12;
    String f13;
    String f14;
    String f15;
    String f16;
    String f17;
    String f18;
    String f19;
    String f20;
    String f21;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
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
