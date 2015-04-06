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
import java.sql.SQLException;
import java.util.HashMap;
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

    // Create DDL sections with their own capture groups
    HostSectionDDL hostSectionDDL = new HostSectionDDL();
    WidgetSectionDDL widgetSectionDDL = new WidgetSectionDDL();
    ViewSectionDDL viewSectionDDL = new ViewSectionDDL();

    // Execute any DDL schema changes
    hostSectionDDL.execute(dbAccessor);
    widgetSectionDDL.execute(dbAccessor);
    viewSectionDDL.execute(dbAccessor);

    // Replay sections
    replay(dbAccessor, configuration, resultSet);

    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration, resultSet);

    // Verify sections
    hostSectionDDL.verify(dbAccessor);
    widgetSectionDDL.verify(dbAccessor);
    viewSectionDDL.verify(dbAccessor);
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

  // *********** Inner Classes that represent sections of the DDL ***********
  // ************************************************************************

  /**
   * Verify that all of the host-related tables added a column for the host_id
   */
  class HostSectionDDL implements SectionDDL {

    HashMap<String, Capture<DBColumnInfo>> captures;

    public HostSectionDDL() {
      // Capture all tables that will have the host_id column added to it.
      captures = new HashMap<String, Capture<DBColumnInfo>>();

      // Column Capture section
      // Hosts
      Capture<DBAccessor.DBColumnInfo> hostsColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostComponentStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostComponentDesiredStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> clusterHostMappingColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

      // TODO, include other tables.
      captures.put("hosts", hostsColumnCapture);
      captures.put("hostcomponentstate", hostComponentStateColumnCapture);
      captures.put("hostcomponentdesiredstate", hostComponentDesiredStateColumnCapture);
      captures.put("hoststate", hostStateColumnCapture);
      captures.put("ClusterHostMapping", clusterHostMappingColumnCapture);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      // Add columns and alter table section
      dbAccessor.addColumn(eq("hosts"), capture(captures.get("hosts")));
      dbAccessor.addColumn(eq("hostcomponentstate"), capture(captures.get("hostcomponentstate")));
      dbAccessor.addColumn(eq("hostcomponentdesiredstate"), capture(captures.get("hostcomponentdesiredstate")));
      dbAccessor.addColumn(eq("hoststate"), capture(captures.get("hoststate")));
      dbAccessor.addColumn(eq("ClusterHostMapping"), capture(captures.get("ClusterHostMapping")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      // Verification section
      for (Capture<DBColumnInfo> columnCapture : captures.values()) {
        verifyContainsHostIdColumn(columnCapture);
      }
    }

    /**
     * Verify that the column capture of the table contains a host_id column of type Long.
     * This is needed for all of the host-related tables that are switching from the
     * host_name to the host_id.
     * @param columnCapture
     */
    private void verifyContainsHostIdColumn(Capture<DBAccessor.DBColumnInfo> columnCapture) {
      DBColumnInfo idColumn = columnCapture.getValue();
      Assert.assertEquals(Long.class, idColumn.getType());
      Assert.assertEquals("host_id", idColumn.getName());
    }
  }

  /**
   * Verify that the user_widget, widget_layout, and widget_layout_user_widget tables are created correctly.
   */
  class WidgetSectionDDL implements SectionDDL {

    HashMap<String, Capture<List<DBColumnInfo>>> captures;

    public WidgetSectionDDL() {
      captures = new HashMap<String, Capture<List<DBColumnInfo>>>();

      Capture<List<DBColumnInfo>> userWidgetColumnsCapture = new Capture<List<DBColumnInfo>>();
      Capture<List<DBColumnInfo>> widgetLayoutColumnsCapture = new Capture<List<DBColumnInfo>>();
      Capture<List<DBColumnInfo>> widgetLayoutUserWidgetColumnsCapture = new Capture<List<DBColumnInfo>>();

      captures.put("user_widget", userWidgetColumnsCapture);
      captures.put("widget_layout", widgetLayoutColumnsCapture);
      captures.put("widget_layout_user_widget", widgetLayoutUserWidgetColumnsCapture);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      Capture<List<DBColumnInfo>> userWidgetColumnsCapture = captures.get("user_widget");
      Capture<List<DBColumnInfo>> widgetLayoutColumnsCapture = captures.get("widget_layout");
      Capture<List<DBColumnInfo>> widgetLayoutUserWidgetColumnsCapture = captures.get("widget_layout_user_widget");

      // User Widget
      dbAccessor.createTable(eq("user_widget"),
          capture(userWidgetColumnsCapture), eq("id"));

      // Widget Layout
      dbAccessor.createTable(eq("widget_layout"),
          capture(widgetLayoutColumnsCapture), eq("id"));

      // Widget Layout User Widget
      dbAccessor.createTable(eq("widget_layout_user_widget"),
          capture(widgetLayoutUserWidgetColumnsCapture), eq("widget_layout_id"), eq("user_widget_id"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      Capture<List<DBColumnInfo>> userWidgetColumnsCapture = captures.get("user_widget");
      Capture<List<DBColumnInfo>> widgetLayoutColumnsCapture = captures.get("widget_layout");
      Capture<List<DBColumnInfo>> widgetLayoutUserWidgetColumnsCapture = captures.get("widget_layout_user_widget");

      // Verify widget tables
      assertEquals(12, userWidgetColumnsCapture.getValue().size());
      assertEquals(7, widgetLayoutColumnsCapture.getValue().size());
      assertEquals(3, widgetLayoutUserWidgetColumnsCapture.getValue().size());
    }
  }

  /**
   * Verify view changes
   */
  class ViewSectionDDL implements SectionDDL {

    HashMap<String, Capture<DBColumnInfo>> captures;

    public ViewSectionDDL() {
      captures = new HashMap<String, Capture<DBColumnInfo>>();

      Capture<DBAccessor.DBColumnInfo> viewInstanceColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> viewParamColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

      captures.put("viewinstance", viewInstanceColumnCapture);
      captures.put("viewparameter", viewParamColumnCapture);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      Capture<DBColumnInfo> viewInstanceColumnCapture = captures.get("viewinstance");
      Capture<DBColumnInfo> viewParamColumnCapture = captures.get("viewparameter");

      dbAccessor.addColumn(eq("viewinstance"), capture(viewInstanceColumnCapture));
      dbAccessor.addColumn(eq("viewparameter"), capture(viewParamColumnCapture));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      verifyViewInstance(captures.get("viewinstance"));
      verifyViewParameter(captures.get("viewparameter"));
    }

    private void verifyViewInstance(Capture<DBAccessor.DBColumnInfo> viewInstanceColumnCapture) {
      DBColumnInfo clusterIdColumn = viewInstanceColumnCapture.getValue();
      Assert.assertEquals(String.class, clusterIdColumn.getType());
      Assert.assertEquals("cluster_handle", clusterIdColumn.getName());
    }


    private void verifyViewParameter(Capture<DBAccessor.DBColumnInfo> viewParamColumnCapture) {
      DBColumnInfo clusterConfigColumn = viewParamColumnCapture.getValue();
      Assert.assertEquals(String.class, clusterConfigColumn.getType());
      Assert.assertEquals("cluster_config", clusterConfigColumn.getName());
    }
  }
}
