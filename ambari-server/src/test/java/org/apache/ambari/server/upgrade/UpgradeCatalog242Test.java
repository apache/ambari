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

package org.apache.ambari.server.upgrade;

import javax.persistence.EntityManager;
import junit.framework.Assert;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Method;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
/**
 * {@link org.apache.ambari.server.upgrade.UpgradeCatalog242} unit tests.
 */
public class UpgradeCatalog242Test {
  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);
  private UpgradeCatalogHelper upgradeCatalogHelper;
  private StackEntity desiredStackEntity;
  private AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
  private AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
  private StackDAO stackDAO = createNiceMock(StackDAO.class);
  private RepositoryVersionDAO repositoryVersionDAO = createNiceMock(RepositoryVersionDAO.class);
  private ClusterVersionDAO clusterVersionDAO = createNiceMock(ClusterVersionDAO.class);
  private HostVersionDAO hostVersionDAO = createNiceMock(HostVersionDAO.class);
  private ClusterDAO clusterDAO = createNiceMock(ClusterDAO.class);

  private IMocksControl mocksControl = EasyMock.createControl();

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    upgradeCatalogHelper = injector.getInstance(UpgradeCatalogHelper.class);
    // inject AmbariMetaInfo to ensure that stacks get populated in the DB
    injector.getInstance(AmbariMetaInfo.class);
    // load the stack entity
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    desiredStackEntity = stackDAO.find("HDP", "2.2.0");
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testUpdateTablesForMysql() throws Exception{
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    final Configuration configuration = createNiceMock(Configuration.class);

    Capture<DBAccessor.DBColumnInfo> extensionExtensionNameColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> extensionExtensionVersionColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> usersUserTypeColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> usersUserNameColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> hostRoleCommandRoleColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> hostRoleCommandStatusColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> blueprintBlueprintNameColumnChangeSize = newCapture();


    expect(configuration.getDatabaseType()).andReturn(Configuration.DatabaseType.MYSQL).once();

    dbAccessor.alterColumn(eq(UpgradeCatalog242.EXTENSION_TABLE), capture(extensionExtensionNameColumnChangeSize));
    dbAccessor.alterColumn(eq(UpgradeCatalog242.EXTENSION_TABLE), capture(extensionExtensionVersionColumnChangeSize));

    dbAccessor.alterColumn(eq(UpgradeCatalog242.USERS_TABLE), capture(usersUserTypeColumnChangeSize));
    dbAccessor.alterColumn(eq(UpgradeCatalog242.USERS_TABLE), capture(usersUserNameColumnChangeSize));

    dbAccessor.alterColumn(eq(UpgradeCatalog242.HOST_ROLE_COMMAND_TABLE), capture(hostRoleCommandRoleColumnChangeSize));
    dbAccessor.alterColumn(eq(UpgradeCatalog242.HOST_ROLE_COMMAND_TABLE), capture(hostRoleCommandStatusColumnChangeSize));

    dbAccessor.alterColumn(eq(UpgradeCatalog242.BLUEPRINT_TABLE), capture(blueprintBlueprintNameColumnChangeSize));


    replay(dbAccessor, configuration);
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(Configuration.class).toInstance(configuration);
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog242 upgradeCatalog242 = injector.getInstance(UpgradeCatalog242.class);
    upgradeCatalog242.executeDDLUpdates();

    DBAccessor.DBColumnInfo extensionExtensionNameInfo = extensionExtensionNameColumnChangeSize.getValue();
    Assert.assertNotNull(extensionExtensionNameInfo);
    Assert.assertEquals(UpgradeCatalog242.EXTENSION_NAME_COLUMN, extensionExtensionNameInfo.getName());
    Assert.assertEquals(new Integer(100), extensionExtensionNameInfo.getLength());
    Assert.assertEquals(String.class, extensionExtensionNameInfo.getType());
    Assert.assertEquals(null, extensionExtensionNameInfo.getDefaultValue());
    Assert.assertEquals(false, extensionExtensionNameInfo.isNullable());

    DBAccessor.DBColumnInfo extensionExtensionVersionInfo = extensionExtensionVersionColumnChangeSize.getValue();
    Assert.assertNotNull(extensionExtensionVersionInfo);
    Assert.assertEquals(UpgradeCatalog242.EXTENSION_VERSION_COLUMN, extensionExtensionVersionInfo.getName());
    Assert.assertEquals(new Integer(100), extensionExtensionVersionInfo.getLength());
    Assert.assertEquals(String.class, extensionExtensionVersionInfo.getType());
    Assert.assertEquals(null, extensionExtensionVersionInfo.getDefaultValue());
    Assert.assertEquals(false, extensionExtensionVersionInfo.isNullable());

    DBAccessor.DBColumnInfo usersUserTypeInfo = usersUserTypeColumnChangeSize.getValue();
    Assert.assertNotNull(usersUserTypeInfo);
    Assert.assertEquals(UpgradeCatalog242.USER_TYPE_COLUMN, usersUserTypeInfo.getName());
    Assert.assertEquals(new Integer(100), usersUserTypeInfo.getLength());
    Assert.assertEquals(String.class, usersUserTypeInfo.getType());
    Assert.assertEquals(null, usersUserTypeInfo.getDefaultValue());
    Assert.assertEquals(false, usersUserTypeInfo.isNullable());

    DBAccessor.DBColumnInfo usersUserNameInfo = usersUserNameColumnChangeSize.getValue();
    Assert.assertNotNull(usersUserNameInfo);
    Assert.assertEquals(UpgradeCatalog242.USER_NAME_COLUMN, usersUserNameInfo.getName());
    Assert.assertEquals(new Integer(100), usersUserNameInfo.getLength());
    Assert.assertEquals(String.class, usersUserNameInfo.getType());
    Assert.assertEquals(null, usersUserNameInfo.getDefaultValue());
    Assert.assertEquals(false, usersUserNameInfo.isNullable());

    DBAccessor.DBColumnInfo hostRoleCommandRoleInfo = hostRoleCommandRoleColumnChangeSize.getValue();
    Assert.assertNotNull(hostRoleCommandRoleInfo);
    Assert.assertEquals(UpgradeCatalog242.ROLE_COLUMN, hostRoleCommandRoleInfo.getName());
    Assert.assertEquals(new Integer(100), hostRoleCommandRoleInfo.getLength());
    Assert.assertEquals(String.class, hostRoleCommandRoleInfo.getType());
    Assert.assertEquals(null, hostRoleCommandRoleInfo.getDefaultValue());
    Assert.assertEquals(true, hostRoleCommandRoleInfo.isNullable());

    DBAccessor.DBColumnInfo hostRoleCommandStatusInfo = hostRoleCommandStatusColumnChangeSize.getValue();
    Assert.assertNotNull(hostRoleCommandStatusInfo);
    Assert.assertEquals(UpgradeCatalog242.STATUS_COLUMN, hostRoleCommandStatusInfo.getName());
    Assert.assertEquals(new Integer(100), hostRoleCommandStatusInfo.getLength());
    Assert.assertEquals(String.class, hostRoleCommandStatusInfo.getType());
    Assert.assertEquals(null, hostRoleCommandStatusInfo.getDefaultValue());
    Assert.assertEquals(true, hostRoleCommandStatusInfo.isNullable());

    DBAccessor.DBColumnInfo blueprintBlueprintNameInfo = blueprintBlueprintNameColumnChangeSize.getValue();
    Assert.assertNotNull(blueprintBlueprintNameInfo);
    Assert.assertEquals(UpgradeCatalog242.BLUEPRINT_NAME_COLUMN, blueprintBlueprintNameInfo.getName());
    Assert.assertEquals(new Integer(100), blueprintBlueprintNameInfo.getLength());
    Assert.assertEquals(String.class, blueprintBlueprintNameInfo.getType());
    Assert.assertEquals(null, blueprintBlueprintNameInfo.getDefaultValue());
    Assert.assertEquals(false, blueprintBlueprintNameInfo.isNullable());


    verify(dbAccessor, configuration);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");


    UpgradeCatalog242 upgradeCatalog242 = createMockBuilder(UpgradeCatalog242.class)
            .addMockedMethod(addNewConfigurationsFromXml)
            .createMock();


    upgradeCatalog242.addNewConfigurationsFromXml();
    expectLastCall().once();


    replay(upgradeCatalog242);

    upgradeCatalog242.executeDMLUpdates();

    verify(upgradeCatalog242);
  }
}
