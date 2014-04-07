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

package org.apache.ambari.server.orm;

import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.Assert;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

public class DBAccessorImplTest {
  private Injector injector;
  private static final AtomicInteger counter = new AtomicInteger(1);

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
  }

  @After
  public void tearDown() throws Exception {

  }

  private static String getFreeTableName() {
    return "test_table_" + counter.getAndIncrement();
  }

  private void createMyTable(String tableName) throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("name", String.class, 20000, null, true));
    columns.add(new DBColumnInfo("time", Long.class, null, null, true));

    dbAccessor.createTable(tableName, columns, "id");
  }

  @Test
  public void testCreateTable() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute(String.format("insert into %s(id, name) values(1,'hello')", tableName));

    ResultSet resultSet = statement.executeQuery(String.format("select * from %s", tableName));

    int count = 0;
    while (resultSet.next()) {
      assertEquals(resultSet.getString("name"), "hello");
      count++;
    }

    assertEquals(count, 1);
  }

  @Test
  public void testAddFKConstraint() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("fid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fname", String.class, null, null, false));

    String foreignTableName = getFreeTableName();
    dbAccessor.createTable(foreignTableName, columns, "fid");

    dbAccessor.addFKConstraint(foreignTableName, "MYFKCONSTRAINT", "fid",
      tableName, "id", false);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("insert into " + tableName + "(id, name) values(1,'hello')");
    statement.execute("insert into " + foreignTableName + "(fid, fname) values(1,'howdy')");

    ResultSet resultSet = statement.executeQuery("select * from " + foreignTableName);

    int count = 0;
    while (resultSet.next()) {
      assertEquals(resultSet.getString("fname"), "howdy");
      count++;
    }
    resultSet.close();
    assertEquals(count, 1);

    exception.expect(SQLException.class);
    exception.expectMessage(containsString("MYFKCONSTRAINT"));
    dbAccessor.dropTable(tableName);
  }

  @Test
  public void testAddColumn() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    DBColumnInfo dbColumnInfo = new DBColumnInfo("description", String.class,
      null, null, true);

    dbAccessor.addColumn(tableName, dbColumnInfo);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("update " + tableName + " set description = 'blah' where id = 1");

    ResultSet resultSet = statement.executeQuery("select description from " + tableName);

    while (resultSet.next()) {
      assertEquals(resultSet.getString("description"), "blah");
    }
    resultSet.close();
  }

  @Test
  public void testUpdateTable() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.updateTable(tableName, "name", "blah", "where id = 1");

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("select name from " + tableName);

    while (resultSet.next()) {
      assertEquals(resultSet.getString("name"), "blah");
    }
    resultSet.close();
  }


  @Test
  public void testTableHasFKConstraint() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("fid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fname", String.class, null, null, false));

    String foreignTableName = getFreeTableName();
    dbAccessor.createTable(foreignTableName, columns, "fid");

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("ALTER TABLE " + foreignTableName + " ADD CONSTRAINT FK_test FOREIGN KEY (fid) REFERENCES " +
      tableName + " (id)");

    Assert.assertTrue(dbAccessor.tableHasForeignKey(foreignTableName,
      tableName, "fid", "id"));
  }

  @Test
  public void testTableExists() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    Statement statement = dbAccessor.getConnection().createStatement();
    String tableName = getFreeTableName();
    statement.execute("Create table " + tableName + " (id VARCHAR(255))");

    Assert.assertTrue(dbAccessor.tableExists(tableName));
  }

  @Test
  public void testColumnExists() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    Assert.assertTrue(dbAccessor.tableHasColumn(tableName, "time"));
  }

  @Test
  public void testRenameColumn() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.executeQuery("insert into " + tableName + "(id, name, time) values(1, 'Bob', 1234567)");

    dbAccessor.renameColumn(tableName, "time", new DBColumnInfo("new_time", Long.class, 0, null, true));

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("select new_time from " + tableName + " where id=1");
    int count = 0;
    while (resultSet.next()) {
      count++;
      long newTime = resultSet.getLong("new_time");
      assertEquals(newTime, 1234567L);
    }

    assertEquals(count, 1);
  }

  @Test
  public void testModifyColumn() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.executeQuery("insert into " + tableName + "(id, name, time) values(1, 'Bob', 1234567)");

    dbAccessor.alterColumn(tableName, new DBColumnInfo("name", String.class, 25000));

  }

  @Test
  public void testAddColumnWithDefault() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.executeQuery("insert into " + tableName + "(id, name, time) values(1, 'Bob', 1234567)");

    dbAccessor.addColumn(tableName, new DBColumnInfo("test", String.class, 1000, "test", false));

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("select * from " + tableName);
    int count = 0;
    while (resultSet.next()) {
      assertEquals(resultSet.getString("test"), "test");
      count++;
    }

    assertEquals(count, 1);

  }

  @Test
  public void testExecuteSelect() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    String tableName = getFreeTableName();
    createMyTable(tableName);
    dbAccessor.executeQuery("insert into " + tableName + "(id, name, time) values(1, 'Bob', 1234567)");

    ResultSet resultSet = dbAccessor.executeSelect("select name from " + tableName + " where id=1");
    int count = 0;
    while (resultSet.next()) {
      assertEquals("Bob", resultSet.getString(1));
      count++;
    }

    assertEquals(count, 1);
  }

  @Test
  public void testDBSession() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    String tableName = getFreeTableName();
    createMyTable(tableName);
    dbAccessor.executeQuery("insert into " + tableName + "(id, name, time) values(1, 'Bob', 1234567)");

    DatabaseSession databaseSession = dbAccessor.getNewDatabaseSession();
    databaseSession.login();
    Vector vector = databaseSession.executeSQL("select * from " + tableName + " where id=1");
    assertEquals(vector.size(), 1);
    Map map = (Map) vector.get(0);
    //all names seem to be converted to upper case
    assertEquals("Bob", map.get("name".toUpperCase()));

    databaseSession.logout();
  }
}
