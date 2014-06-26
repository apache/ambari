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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * UpgradeCatalog170 unit tests.
 */
public class UpgradeCatalog170Test {

  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Configuration configuration = createNiceMock(Configuration.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

    Capture<DBAccessor.DBColumnInfo> clusterConfigAttributesColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

    setClusterConfigExpectations(dbAccessor, clusterConfigAttributesColumnCapture);
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

    assertClusterConfigColumns(clusterConfigAttributesColumnCapture);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
  }


  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog   upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("1.7.0", upgradeCatalog.getTargetVersion());
  }

  private AbstractUpgradeCatalog getUpgradeCatalog(final DBAccessor dbAccessor) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
      }
    };
    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog170.class);
  }
  
  private void assertClusterConfigColumns(Capture<DBAccessor.DBColumnInfo> clusterConfigAttributesColumnCapture) {
    DBAccessor.DBColumnInfo column = clusterConfigAttributesColumnCapture.getValue();
    assertEquals("config_attributes", column.getName());
    assertEquals(32000, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertEquals(null, column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void setClusterConfigExpectations(DBAccessor dbAccessor,
                                   Capture<DBAccessor.DBColumnInfo> clusterConfigAttributesColumnCapture)
      throws SQLException {
    dbAccessor.addColumn(eq("clusterconfig"),
        capture(clusterConfigAttributesColumnCapture));
  }
  
  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("1.6.1", upgradeCatalog.getSourceVersion());
  }   
}
