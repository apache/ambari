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

import static org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

public class DBAccessorImplTest {
  private Injector injector;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
  }

  @After
  public void tearDown() throws Exception {

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
    createMyTable("mytable1");
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("insert into mytable1(id, name) values(1,'hello')");

    ResultSet resultSet = statement.executeQuery("select * from mytable1");

    int count = 0;
    while (resultSet.next()) {
      assertEquals(resultSet.getString("name"), "hello");
      count++;
    }

    assertEquals(count, 1);
  }

  @Test
  public void testAddFKConstraint() throws Exception {
    createMyTable("mytable2");
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("fid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fname", String.class, null, null, false));

    dbAccessor.createTable("foreigntable", columns, "fid");

    dbAccessor.addFKConstraint("foreigntable", "MYFKCONSTRAINT", "fid",
      "mytable2", "id", false);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("insert into mytable2(id, name) values(1,'hello')");
    statement.execute("insert into foreigntable(fid, fname) values(1,'howdy')");

    ResultSet resultSet = statement.executeQuery("select * from foreigntable");

    int count = 0;
    while (resultSet.next()) {
      assertEquals(resultSet.getString("fname"), "howdy");
      count++;
    }
    resultSet.close();
    assertEquals(count, 1);

    exception.expect(SQLException.class);
    exception.expectMessage(containsString("MYFKCONSTRAINT"));
    dbAccessor.dropTable("mytable2");
  }

  @Test
  public void testAddColumn() throws Exception {
    createMyTable("mytable3");
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    DBColumnInfo dbColumnInfo = new DBColumnInfo("description", String.class,
      null, null, true);

    dbAccessor.addColumn("mytable3", dbColumnInfo);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("update mytable3 set description = 'blah' where id = 1");

    ResultSet resultSet = statement.executeQuery("select description from mytable3");

    while (resultSet.next()) {
      assertEquals(resultSet.getString("description"), "blah");
    }
    resultSet.close();
  }

  @Test
  public void testUpdateTable() throws Exception {
    createMyTable("mytable4");
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.updateTable("mytable4", "name", "blah", "where id = 1");

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("select name from mytable4");

    while (resultSet.next()) {
      assertEquals(resultSet.getString("name"), "blah");
    }
    resultSet.close();
  }

  @Test
  public void testRenameColumn() throws Exception {
    createMyTable("mytable6");
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.executeQuery("insert into mytable6(id, name, time) values(1, 'Bob', 1234567)");

    dbAccessor.renameColumn("mytable6", "time", new DBColumnInfo("new_time", Long.class, 0, null, true));

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("select new_time from mytable6 where id=1");
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
    createMyTable("mytable7");
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.executeQuery("insert into mytable7(id, name, time) values(1, 'Bob', 1234567)");

    dbAccessor.alterColumn("mytable7", new DBColumnInfo("name", String.class, 25000));

  }

  @Test
  public void testAddColumnWithDefault() throws Exception {
    createMyTable("mytable8");
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.executeQuery("insert into mytable8(id, name, time) values(1, 'Bob', 1234567)");

    dbAccessor.addColumn("mytable8", new DBColumnInfo("test", String.class, 1000, "test", false));

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("select * from mytable8");
    int count = 0;
    while (resultSet.next()) {
      assertEquals(resultSet.getString("test"), "test");
      count++;
    }

    assertEquals(count, 1);

  }

  @Ignore // Not working with derby db driver
  @Test
  public void testTableHasFKConstraint() throws Exception {
    createMyTable("mytable5");

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<DBColumnInfo>();
    columns.add(new DBColumnInfo("fid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fname", String.class, null, null, false));

    dbAccessor.createTable("foreigntable5", columns, "fid");

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("ALTER TABLE foreigntable5 ADD CONSTRAINT FK_test FOREIGN KEY (fid) REFERENCES mytable5 (id)");

    Assert.assertTrue(dbAccessor.tableHasForeignKey("foreigntable5",
      "mytable5", "fid", "id"));
  }

  @Test
  public void testTableExists() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("Create table testTable (id VARCHAR(255))");

    Assert.assertTrue(dbAccessor.tableExists("testTable"));
  }

  @Test
  public void testColumnExists() throws Exception {
    createMyTable("mytable6");

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    Assert.assertTrue(dbAccessor.tableHasColumn("mytable6", "time"));
  }

}
