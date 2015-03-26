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

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import static org.easymock.EasyMock.capture;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * {@link org.apache.ambari.server.upgrade.UpgradeCatalog210} unit tests.
 */
public class UpgradeCatalog210Test {
  private final String CLUSTER_NAME = "c1";
  private final String HOST_NAME = "h1";
  private final String DESIRED_STACK_VERSION = "{\"stackName\":\"HDP\",\"stackVersion\":\"2.0.6\"}";

  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);
  private UpgradeCatalogHelper upgradeCatalogHelper;

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    upgradeCatalogHelper = injector.getInstance(UpgradeCatalogHelper.class);
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Connection connection = createNiceMock(Connection.class);
    Configuration configuration = createNiceMock(Configuration.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

    HostDAO hostDao = createNiceMock(HostDAO.class);
    HostEntity mockHost = createNiceMock(HostEntity.class);
    expect(hostDao.findByName("foo")).andReturn(mockHost).anyTimes();

    // Column Capture section
    Capture<DBAccessor.DBColumnInfo> hostsColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

    // Add columns and alter table section
    dbAccessor.addColumn(eq("hosts"), capture(hostsColumnCapture));

    Capture<List<DBColumnInfo>> userWidgetColumnsCapture = new Capture<List<DBColumnInfo>>();
    Capture<List<DBColumnInfo>> widgetLayoutColumnsCapture = new Capture<List<DBColumnInfo>>();
    Capture<List<DBColumnInfo>> widgetLayoutUserWidgetColumnsCapture = new Capture<List<DBColumnInfo>>();

    // User Widget
    dbAccessor.createTable(eq("user_widget"),
        capture(userWidgetColumnsCapture), eq("id"));

    // Widget Layout
    dbAccessor.createTable(eq("widget_layout"),
            capture(widgetLayoutColumnsCapture), eq("id"));

    // Widget Layout User Widget
    dbAccessor.createTable(eq("widget_layout_user_widget"),
            capture(widgetLayoutUserWidgetColumnsCapture), eq("widget_layout_id"), eq("user_widget_id"));

    // Replay section
    replay(dbAccessor, configuration, resultSet);
    replay(hostDao, mockHost);

    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration, resultSet);

    // Verification section
    verifyHosts(hostsColumnCapture);


    // Verify widget tables
    assertEquals(12, userWidgetColumnsCapture.getValue().size());
    assertEquals(4, widgetLayoutColumnsCapture.getValue().size());
    assertEquals(3, widgetLayoutUserWidgetColumnsCapture.getValue().size());
  }

  private void verifyHosts(Capture<DBAccessor.DBColumnInfo> hostsColumnCapture) {
    DBColumnInfo hostsIdColumn = hostsColumnCapture.getValue();
    Assert.assertEquals(Long.class, hostsIdColumn.getType());
    Assert.assertEquals("id", hostsIdColumn.getName());
  }

  /**
   * @param dbAccessor
   * @return
   */
  private AbstractUpgradeCatalog getUpgradeCatalog(final DBAccessor dbAccessor) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    };

    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog210.class);
  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("2.0.0", upgradeCatalog.getSourceVersion());
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("2.1.0", upgradeCatalog.getTargetVersion());
  }
}
