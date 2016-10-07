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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class UpgradeCatalog300Test {

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method showHcatDeletedUserMessage = UpgradeCatalog300.class.getDeclaredMethod("showHcatDeletedUserMessage");

   UpgradeCatalog300 upgradeCatalog300 = createMockBuilder(UpgradeCatalog300.class)
            .addMockedMethod(showHcatDeletedUserMessage)
            .addMockedMethod(addNewConfigurationsFromXml)
            .createMock();


    upgradeCatalog300.addNewConfigurationsFromXml();
    upgradeCatalog300.showHcatDeletedUserMessage();


    replay(upgradeCatalog300);

    upgradeCatalog300.executeDMLUpdates();

    verify(upgradeCatalog300);
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final EntityManager entityManager = createNiceMock(EntityManager.class);
    final DBAccessor dbAccessor = createStrictMock(DBAccessor.class);
    Configuration configuration = createNiceMock(Configuration.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);


    // !!! setup capture for servicecomponent_version
    Capture<List<DBAccessor.DBColumnInfo>> capturedComponentVersionColumns = newCapture();

    dbAccessor.createTable(eq(UpgradeCatalog300.COMPONENT_VERSION_TABLE), capture(capturedComponentVersionColumns),
        eq((String[]) null));

    dbAccessor.addPKConstraint(eq(UpgradeCatalog300.COMPONENT_VERSION_TABLE),
        eq(UpgradeCatalog300.COMPONENT_VERSION_PK), eq("id"));
    dbAccessor.addFKConstraint(eq(UpgradeCatalog300.COMPONENT_VERSION_TABLE),
        eq(UpgradeCatalog300.COMPONENT_VERSION_FK_COMPONENT), eq("component_id"),
        eq(UpgradeCatalog300.COMPONENT_TABLE), eq("id"), eq(false));
    dbAccessor.addFKConstraint(eq(UpgradeCatalog300.COMPONENT_VERSION_TABLE),
        eq(UpgradeCatalog300.COMPONENT_VERSION_FK_REPO_VERSION), eq("repo_version_id"),
        eq("repo_version"), eq("id"), eq(false));


    expect(dbAccessor.getConnection()).andReturn(connection);
    expect(connection.createStatement()).andReturn(statement);
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet);

    replay(dbAccessor, configuration, connection, statement, resultSet);

    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog300 upgradeCatalog300 = injector.getInstance(UpgradeCatalog300.class);
    upgradeCatalog300.executeDDLUpdates();

    verify(dbAccessor);

    // !!! check the captured for servicecomponent_version
    Map<String, DBAccessor.DBColumnInfo> expected = new HashMap<>();
    expected.put("id", new DBAccessor.DBColumnInfo("id", Long.class, null, null, false));
    expected.put("component_id", new DBAccessor.DBColumnInfo("component_id", Long.class, null, null, false));
    expected.put("repo_version_id", new DBAccessor.DBColumnInfo("repo_version_id", Long.class, null, null, false));
    expected.put("state", new DBAccessor.DBColumnInfo("state", String.class, 32, null, false));
    expected.put("user_name", new DBAccessor.DBColumnInfo("user_name", String.class, 255, null, false));

    List<DBAccessor.DBColumnInfo> captured = capturedComponentVersionColumns.getValue();
    Assert.assertEquals(5, captured.size());

    for (DBAccessor.DBColumnInfo column : captured) {
      DBAccessor.DBColumnInfo expectedColumn = expected.remove(column.getName());

      Assert.assertNotNull(expectedColumn);
      Assert.assertEquals(expectedColumn.getDefaultValue(), column.getDefaultValue());
      Assert.assertEquals(expectedColumn.getName(), column.getName());
      Assert.assertEquals(expectedColumn.getLength(), column.getLength());
      Assert.assertEquals(expectedColumn.getType(), column.getType());
      Assert.assertEquals(expectedColumn.getClass(), column.getClass());
    }

    // did we get them all?
    Assert.assertEquals(0, expected.size());
  }

}
