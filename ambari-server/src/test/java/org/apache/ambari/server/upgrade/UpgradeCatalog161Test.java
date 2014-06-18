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
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.state.State;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.*;

/**
 * UpgradeCatalog161 unit tests.
 */
public class UpgradeCatalog161Test {

  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Configuration configuration = createNiceMock(Configuration.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

    Capture<DBAccessor.DBColumnInfo> provisioningStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<List<DBAccessor.DBColumnInfo>> operationLevelEntityColumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<DBAccessor.DBColumnInfo> labelColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> descriptionColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> visibleColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> viewIconColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> viewIcon64ColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> instanceIconColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> instanceIcon64ColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

    setClustersConfigExpectations(dbAccessor, provisioningStateColumnCapture);    
    setOperationLevelEntityConfigExpectations(dbAccessor, operationLevelEntityColumnCapture);
    setViewExpectations(dbAccessor, viewIconColumnCapture, viewIcon64ColumnCapture);
    dbAccessor.addColumn(eq("viewinstance"),
        anyObject(DBAccessor.DBColumnInfo.class));
    setViewInstanceExpectations(dbAccessor, labelColumnCapture, descriptionColumnCapture, visibleColumnCapture, instanceIconColumnCapture, instanceIcon64ColumnCapture);
    dbAccessor.executeSelect(anyObject(String.class));
    expectLastCall().andReturn(resultSet).anyTimes();
    resultSet.next();
    expectLastCall().andReturn(false).anyTimes();
    resultSet.close();
    expectLastCall().anyTimes();

    replay(dbAccessor, configuration, resultSet);
    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration, resultSet);

    assertClusterColumns(provisioningStateColumnCapture);
    assertOperationLevelEntityColumns(operationLevelEntityColumnCapture);
    assertViewColumns(viewIconColumnCapture, viewIcon64ColumnCapture);
    assertViewInstanceColumns(labelColumnCapture, descriptionColumnCapture, visibleColumnCapture, instanceIconColumnCapture, instanceIcon64ColumnCapture);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Injector injector = createStrictMock(Injector.class);
    Provider provider = createStrictMock(Provider.class);
    EntityManager em = createStrictMock(EntityManager.class);
    EntityTransaction et = createMock(EntityTransaction.class);
    TypedQuery query = createMock(TypedQuery.class);

    UpgradeCatalog161 upgradeCatalog =
      createMockBuilder(UpgradeCatalog161.class).createMock();

    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();
    expect(injector.getProvider(EntityManager.class)).andReturn(provider).anyTimes();
    expect(provider.get()).andReturn(em).anyTimes();
    expect(em.getTransaction()).andReturn(et);
    expect(et.isActive()).andReturn(true);
    expect(em.createQuery("UPDATE ClusterEntity SET provisioningState = " +
      ":provisioningState", ClusterEntity.class)).andReturn(query);
    expect(query.setParameter("provisioningState", State.INSTALLED)).andReturn(null);
    expect(query.executeUpdate()).andReturn(0);

    replay(upgradeCatalog, dbAccessor, configuration, injector, provider, em,
      et, query);

    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);
    f = c.getDeclaredField("dbAccessor");
    f.setAccessible(true);
    f.set(upgradeCatalog, dbAccessor);
    f = c.getDeclaredField("injector");
    f.setAccessible(true);
    f.set(upgradeCatalog, injector);

    upgradeCatalog.executeDMLUpdates();

    verify(upgradeCatalog, dbAccessor, configuration, injector, provider, em,
      et, query);
  }


  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog   upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("1.6.1", upgradeCatalog.getTargetVersion());
  }


  private AbstractUpgradeCatalog getUpgradeCatalog(final DBAccessor dbAccessor) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
      }
    };
    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog161.class);
  }


  private void setOperationLevelEntityConfigExpectations(DBAccessor dbAccessor,
                    Capture<List<DBAccessor.DBColumnInfo>> operationLevelEntitycolumnCapture)
          throws SQLException {

    dbAccessor.createTable(eq("requestoperationlevel"),
            capture(operationLevelEntitycolumnCapture), eq("operation_level_id"));

    dbAccessor.addFKConstraint("requestoperationlevel", "FK_req_op_level_req_id",
            "request_id", "request", "request_id", true);
  }


  private void assertOperationLevelEntityColumns(Capture<List<DBAccessor.DBColumnInfo>> operationLevelEntitycolumnCapture) {
    List<DBAccessor.DBColumnInfo> columns = operationLevelEntitycolumnCapture.getValue();
    assertEquals(7, columns.size());

    DBAccessor.DBColumnInfo column = columns.get(0);
    assertEquals("operation_level_id", column.getName());
    assertNull(column.getLength());
    assertEquals(Long.class, column.getType());
    assertNull(column.getDefaultValue());
    assertFalse(column.isNullable());

    column = columns.get(1);
    assertEquals("request_id", column.getName());
    assertNull(column.getLength());
    assertEquals(Long.class, column.getType());
    assertNull(column.getDefaultValue());
    assertFalse(column.isNullable());

    column = columns.get(2);
    assertEquals("level_name", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());

    column = columns.get(3);
    assertEquals("cluster_name", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());

    column = columns.get(4);
    assertEquals("service_name", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());

    column = columns.get(5);
    assertEquals("host_component_name", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());

    column = columns.get(6);
    assertEquals("host_name", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void setClustersConfigExpectations(DBAccessor dbAccessor,
      Capture<DBAccessor.DBColumnInfo> provisioningStateColumnCapture) throws SQLException {

      dbAccessor.addColumn(eq("clusters"),
        capture(provisioningStateColumnCapture));
    }
  
  private void assertClusterColumns(
      Capture<DBAccessor.DBColumnInfo> provisiontStateColumnCapture) {
      DBAccessor.DBColumnInfo column = provisiontStateColumnCapture.getValue();
      assertEquals("provisioning_state", column.getName());
      assertEquals(255, (int) column.getLength());
      assertEquals(String.class, column.getType());
      assertEquals(State.INIT.name(), column.getDefaultValue());
      assertFalse(column.isNullable());
    }


  private void setViewExpectations(DBAccessor dbAccessor,
                                   Capture<DBAccessor.DBColumnInfo> viewIconColumnCapture,
                                   Capture<DBAccessor.DBColumnInfo> viewIcon64ColumnCapture)
      throws SQLException {

    dbAccessor.addColumn(eq("viewmain"),
        capture(viewIconColumnCapture));

    dbAccessor.addColumn(eq("viewmain"),
        capture(viewIcon64ColumnCapture));
  }

  private void setViewInstanceExpectations(DBAccessor dbAccessor,
                                           Capture<DBAccessor.DBColumnInfo> labelColumnCapture,
                                           Capture<DBAccessor.DBColumnInfo> descriptionColumnCapture,
                                           Capture<DBAccessor.DBColumnInfo> visibleColumnCapture,
                                           Capture<DBAccessor.DBColumnInfo> instanceIconColumnCapture,
                                           Capture<DBAccessor.DBColumnInfo> instanceIcon64ColumnCapture)
      throws SQLException {

    dbAccessor.addColumn(eq("viewinstance"),
        capture(labelColumnCapture));

    dbAccessor.addColumn(eq("viewinstance"),
        capture(descriptionColumnCapture));

    dbAccessor.addColumn(eq("viewinstance"),
        capture(visibleColumnCapture));

    dbAccessor.addColumn(eq("viewinstance"),
        capture(instanceIconColumnCapture));

    dbAccessor.addColumn(eq("viewinstance"),
        capture(instanceIcon64ColumnCapture));
  }

  private void assertViewColumns(
      Capture<DBAccessor.DBColumnInfo> viewIconColumnCapture,
      Capture<DBAccessor.DBColumnInfo> viewIcon64ColumnCapture) {
    DBAccessor.DBColumnInfo column  = viewIconColumnCapture.getValue();
    assertEquals("icon", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());

    column = viewIcon64ColumnCapture.getValue();
    assertEquals("icon64", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void assertViewInstanceColumns(
      Capture<DBAccessor.DBColumnInfo> labelColumnCapture,
      Capture<DBAccessor.DBColumnInfo> descriptionColumnCapture,
      Capture<DBAccessor.DBColumnInfo> visibleColumnCapture,
      Capture<DBAccessor.DBColumnInfo> instanceIconColumnCapture,
      Capture<DBAccessor.DBColumnInfo> instanceIcon64ColumnCapture) {
    DBAccessor.DBColumnInfo column = labelColumnCapture.getValue();
    assertEquals("label", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  
    column = descriptionColumnCapture.getValue();
    assertEquals("description", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());

    column = visibleColumnCapture.getValue();
    assertEquals("visible", column.getName());
    assertEquals(1, (int) column.getLength());
    assertEquals(Character.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());

    column = instanceIconColumnCapture.getValue();
    assertEquals("icon", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());

    column = instanceIcon64ColumnCapture.getValue();
    assertEquals("icon64", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }
  
  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("1.6.0", upgradeCatalog.getSourceVersion());
  }   
}
