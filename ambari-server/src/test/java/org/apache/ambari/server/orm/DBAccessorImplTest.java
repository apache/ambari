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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.ByteArrayInputStream;
import java.sql.Clob;
import java.sql.PreparedStatement;

public class DBAccessorImplTest {
  private Injector injector;
  private static final AtomicInteger tables_counter = new AtomicInteger(1);
  private static final AtomicInteger schemas_counter = new AtomicInteger(1);

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
    return "test_table_" + tables_counter.getAndIncrement();
  }

  private static String getFreeSchamaName() {
    return "test_schema_" + schemas_counter.getAndIncrement();
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
  public void testDbType() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    assertEquals(DBAccessor.DbType.DERBY, dbAccessor.getDbType());
  }

  @Test
  public void testAlterColumn() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    ResultSet rs;
    DBColumnInfo fromColumn;
    DBColumnInfo toColumn;
    Statement statement = dbAccessor.getConnection().createStatement();
    final String dataString = "Data for inserting column.";

    // 1 - VARACHAR --> VARCHAR
    toColumn = new DBColumnInfo("name", String.class, 500, null, true);
    statement.execute(
        String.format("INSERT INTO %s(id, name) VALUES (1, '%s')", tableName,
            dataString));

    dbAccessor.alterColumn(tableName, toColumn);
    rs = statement.executeQuery(
        String.format("SELECT name FROM %s", tableName));
    while (rs.next()) {
      ResultSetMetaData rsm = rs.getMetaData();
      assertEquals(rs.getString(toColumn.getName()), dataString);
      assertEquals(rsm.getColumnTypeName(1), "VARCHAR");
      assertEquals(rsm.getColumnDisplaySize(1), 500);
    }
    rs.close();

    // 2 - VARACHAR --> CLOB
    toColumn = new DBColumnInfo("name", char[].class, 999, null, true);
    dbAccessor.alterColumn(tableName, toColumn);
    rs = statement.executeQuery(
        String.format("SELECT name FROM %s", tableName));
    while (rs.next()) {
      ResultSetMetaData rsm = rs.getMetaData();
      Clob clob = rs.getClob(toColumn.getName());
      assertEquals(clob.getSubString(1, (int) clob.length()), dataString);
      assertEquals(rsm.getColumnTypeName(1), "CLOB");
      assertEquals(rsm.getColumnDisplaySize(1), 999);
    }
    rs.close();

    // 3 - BLOB --> CLOB
    toColumn = new DBColumnInfo("name_blob_to_clob", char[].class, 567, null,
        true);
    fromColumn = new DBColumnInfo("name_blob_to_clob", byte[].class, 20000,
        null, true);
    dbAccessor.addColumn(tableName, fromColumn);

    String sql = String.format(
        "insert into %s(id, name_blob_to_clob) values (2, ?)", tableName);
    PreparedStatement preparedStatement = dbAccessor.getConnection().prepareStatement(
        sql);
    preparedStatement.setBinaryStream(1,
        new ByteArrayInputStream(dataString.getBytes()),
        dataString.getBytes().length);
    preparedStatement.executeUpdate();
    preparedStatement.close();

    dbAccessor.alterColumn(tableName, toColumn);
    rs = statement.executeQuery(
        String.format("SELECT name_blob_to_clob FROM %s WHERE id=2",
            tableName));
    while (rs.next()) {
      ResultSetMetaData rsm = rs.getMetaData();
      Clob clob = rs.getClob(toColumn.getName());
      assertEquals(clob.getSubString(1, (int) clob.length()), dataString);
      assertEquals(rsm.getColumnTypeName(1), "CLOB");
      assertEquals(rsm.getColumnDisplaySize(1), 567);
    }
    rs.close();

    // 4 - CLOB --> CLOB
    toColumn = new DBColumnInfo("name_blob_to_clob", char[].class, 1500, null,
        true);
    dbAccessor.alterColumn(tableName, toColumn);
    rs = statement.executeQuery(
        String.format("SELECT name_blob_to_clob FROM %s WHERE id=2",
            tableName));
    while (rs.next()) {
      ResultSetMetaData rsm = rs.getMetaData();
      Clob clob = rs.getClob(toColumn.getName());
      assertEquals(clob.getSubString(1, (int) clob.length()), dataString);
      assertEquals(rsm.getColumnTypeName(1), "CLOB");
      assertEquals(rsm.getColumnDisplaySize(1), 1500);
    }
    rs.close();

    dbAccessor.dropTable(tableName);

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
  public void testAddPKConstraint() throws Exception{
    String tableName = getFreeTableName();

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("sid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("data", char[].class, null, null, true));

    dbAccessor.createTable(tableName, columns);

    dbAccessor.addPKConstraint(tableName, "PK_sid", "sid");
    try {
      //List<String> indexes = dbAccessor.getIndexesList(tableName, false);
      //assertTrue(CustomStringUtils.containsCaseInsensitive("pk_sid", indexes));
    } finally {
      dbAccessor.dropTable(tableName);
    }
  }

  @Test
  public void testAddColumn() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    DBColumnInfo dbColumnInfo = new DBColumnInfo("description", String.class,  null, null, true);

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
  public void testGetCheckedForeignKey() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("fid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fname", String.class, null, null, false));

    String foreignTableName = getFreeTableName();
    dbAccessor.createTable(foreignTableName, columns, "fid");

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("ALTER TABLE " + foreignTableName + " ADD CONSTRAINT FK_test1 FOREIGN KEY (fid) REFERENCES " +
            tableName + " (id)");

    Assert.assertEquals("FK_TEST1", dbAccessor.getCheckedForeignKey(foreignTableName, "fk_test1"));
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
  public void testTableExistsMultipleSchemas() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    String tableName = getFreeTableName();
    createMyTable(tableName);

    // create table with the same name but in custom schema
    createTableUnderNewSchema(dbAccessor, tableName);

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
  public void testColumnExistsMultipleSchemas() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    String tableName = getFreeTableName();
    createMyTable(tableName);

    // create table with the same name and same field (id) but in custom schema
    createTableUnderNewSchema(dbAccessor, tableName);

    Assert.assertTrue(dbAccessor.tableHasColumn(tableName, "id"));
  }

  @Test
  public void testColumnsExistsMultipleSchemas() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    String tableName = getFreeTableName();
    createMyTable(tableName);

    // create table with the same name and same field (id) but in custom schema
    createTableUnderNewSchema(dbAccessor, tableName);

    Assert.assertTrue(dbAccessor.tableHasColumn(tableName, "id", "time"));
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

  @Test
  public void testGetColumnType() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    assertEquals(Types.BIGINT, dbAccessor.getColumnType(tableName, "id"));
    assertEquals(Types.VARCHAR, dbAccessor.getColumnType(tableName, "name"));
  }

  @Test
  public void testSetNullable() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.addColumn(tableName, new DBColumnInfo("isNullable",
        String.class, 1000, "test", false));

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("SELECT isNullable FROM "
        + tableName);
    ResultSetMetaData rsmd = resultSet.getMetaData();
    assertEquals(ResultSetMetaData.columnNullable, rsmd.isNullable(1));

    statement.close();

    dbAccessor.setColumnNullable(tableName, new DBColumnInfo("isNullable",
                                                              String.class, 1000, "test", false), false);
    statement = dbAccessor.getConnection().createStatement();
    resultSet = statement.executeQuery("SELECT isNullable FROM " + tableName);
    rsmd = resultSet.getMetaData();
    assertEquals(ResultSetMetaData.columnNoNulls, rsmd.isNullable(1));

    statement.close();

    dbAccessor.setColumnNullable(tableName, new DBColumnInfo("isNullable",
                                                              String.class, 1000, "test", false), true);
    statement = dbAccessor.getConnection().createStatement();
    resultSet = statement.executeQuery("SELECT isNullable FROM " + tableName);
    rsmd = resultSet.getMetaData();
    assertEquals(ResultSetMetaData.columnNullable, rsmd.isNullable(1));

    statement.close();
  }

  private void createTableUnderNewSchema(DBAccessorImpl dbAccessor, String tableName) throws SQLException {
    Statement schemaCreation = dbAccessor.getConnection().createStatement();
    String schemaName = getFreeSchamaName();
    schemaCreation.execute("create schema " + schemaName);

    Statement customSchemaTableCreation = dbAccessor.getConnection().createStatement();
    customSchemaTableCreation.execute(toString().format("Create table %s.%s (id int, time int)", schemaName, tableName));
  }
}
