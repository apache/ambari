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
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * UpgradeCatalog160 unit tests.
 */
public class UpgradeCatalog160Test {

  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Configuration configuration = createNiceMock(Configuration.class);
    Capture<List<DBAccessor.DBColumnInfo>> hgConfigcolumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();

    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

    setBPHostGroupConfigExpectations(dbAccessor, hgConfigcolumnCapture);

    replay(dbAccessor, configuration);
    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration);

    assertHGConfigColumns(hgConfigcolumnCapture);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog160 upgradeCatalog = (UpgradeCatalog160) getUpgradeCatalog(dbAccessor);

    upgradeCatalog.executeDMLUpdates();
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog   upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("1.6.0", upgradeCatalog.getTargetVersion());
  }

  private AbstractUpgradeCatalog getUpgradeCatalog(final DBAccessor dbAccessor) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
      }
    };
    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog160.class);
  }

  private void setBPHostGroupConfigExpectations(DBAccessor dbAccessor, Capture<List<DBAccessor.DBColumnInfo>> columnCapture) throws SQLException {
    dbAccessor.createTable(eq("hostgroup_configuration"), capture(columnCapture),
        eq("blueprint_name"), eq("hostgroup_name"), eq("type_name"));


    dbAccessor.addFKConstraint("hostgroup_configuration", "FK_hg_config_blueprint_name",
        "blueprint_name", "hostgroup", "blueprint_name", true);
    dbAccessor.addFKConstraint("hostgroup_configuration", "FK_hg_config_hostgroup_name",
        "hostgroup_name", "hostgroup", "name", true);
  }

  private void assertHGConfigColumns(Capture<List<DBAccessor.DBColumnInfo>> hgConfigcolumnCapture) {
    List<DBAccessor.DBColumnInfo> columns = hgConfigcolumnCapture.getValue();
    assertEquals(4, columns.size());
    DBAccessor.DBColumnInfo column = columns.get(0);
    assertEquals("blueprint_name", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertFalse(column.isNullable());

    column = columns.get(1);
    assertEquals("hostgroup_name", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertFalse(column.isNullable());

    column = columns.get(2);
    assertEquals("type_name", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertFalse(column.isNullable());

    column = columns.get(3);
    assertEquals("config_data", column.getName());
    assertEquals(null, column.getLength());
    assertEquals(byte[].class, column.getType());
    assertNull(column.getDefaultValue());
    assertFalse(column.isNullable());
  }
}
