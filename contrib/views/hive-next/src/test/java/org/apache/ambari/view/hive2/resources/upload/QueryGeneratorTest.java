/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive2.resources.upload;

import org.apache.ambari.view.hive2.resources.uploads.query.RowFormat;
import org.apache.ambari.view.hive2.client.ColumnDescription;
import org.apache.ambari.view.hive2.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive2.resources.uploads.HiveFileType;
import org.apache.ambari.view.hive2.resources.uploads.query.DeleteQueryInput;
import org.apache.ambari.view.hive2.resources.uploads.query.InsertFromQueryInput;
import org.apache.ambari.view.hive2.resources.uploads.query.QueryGenerator;
import org.apache.ambari.view.hive2.resources.uploads.query.TableInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class QueryGeneratorTest {
  @Test
  public void testCreateTextFile() {

    List<ColumnDescriptionImpl> cdl = new ArrayList<>(4);
    cdl.add(new ColumnDescriptionImpl("col1", ColumnDescription.DataTypes.CHAR.toString(), 0, 10));
    cdl.add(new ColumnDescriptionImpl("col2", ColumnDescription.DataTypes.STRING.toString(), 1));
    cdl.add(new ColumnDescriptionImpl("col3", ColumnDescription.DataTypes.DECIMAL.toString(), 2, 10, 5));
    cdl.add(new ColumnDescriptionImpl("col4", ColumnDescription.DataTypes.VARCHAR.toString(), 3, 40));
    cdl.add(new ColumnDescriptionImpl("col5", ColumnDescription.DataTypes.INT.toString(), 4));

    TableInfo ti = new TableInfo("databaseName", "tableName", cdl, HiveFileType.TEXTFILE, new RowFormat(',', '\\'));

    QueryGenerator qg = new QueryGenerator();
    Assert.assertEquals("Create query for text file not correct ","CREATE TABLE tableName (col1 CHAR(10), col2 STRING," +
      " col3 DECIMAL(10,5), col4 VARCHAR(40), col5 INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ','" +
      " ESCAPED BY '\\\\' STORED AS TEXTFILE;",qg.generateCreateQuery(ti));
  }

  @Test
  public void testCreateORC() {

    List<ColumnDescriptionImpl> cdl = new ArrayList<>(4);
    cdl.add(new ColumnDescriptionImpl("col1", ColumnDescription.DataTypes.CHAR.toString(), 0, 10));
    cdl.add(new ColumnDescriptionImpl("col2", ColumnDescription.DataTypes.STRING.toString(), 1));
    cdl.add(new ColumnDescriptionImpl("col3", ColumnDescription.DataTypes.DECIMAL.toString(), 2, 10, 5));
    cdl.add(new ColumnDescriptionImpl("col4", ColumnDescription.DataTypes.VARCHAR.toString(), 3, 40));
    cdl.add(new ColumnDescriptionImpl("col5", ColumnDescription.DataTypes.INT.toString(), 4));

    TableInfo ti = new TableInfo("databaseName", "tableName", cdl, HiveFileType.ORC, new RowFormat(',', '\\'));

    QueryGenerator qg = new QueryGenerator();
    Assert.assertEquals("Create query for text file not correct ","CREATE TABLE tableName (col1 CHAR(10), col2 STRING, col3 DECIMAL(10,5), col4 VARCHAR(40), col5 INT) STORED AS ORC;",qg.generateCreateQuery(ti));
  }

  @Test
  public void testInsertWithoutUnhexFromQuery() {
    List<ColumnDescriptionImpl> cdl = new ArrayList<>(4);
    cdl.add(new ColumnDescriptionImpl("col1", ColumnDescription.DataTypes.CHAR.toString(), 0, 10));
    cdl.add(new ColumnDescriptionImpl("col2", ColumnDescription.DataTypes.STRING.toString(), 1));
    cdl.add(new ColumnDescriptionImpl("col3", ColumnDescription.DataTypes.DECIMAL.toString(), 2, 10, 5));
    cdl.add(new ColumnDescriptionImpl("col4", ColumnDescription.DataTypes.VARCHAR.toString(), 3, 40));
    cdl.add(new ColumnDescriptionImpl("col5", ColumnDescription.DataTypes.INT.toString(), 4));

    InsertFromQueryInput ifqi = new InsertFromQueryInput("fromDB","fromTable","toDB","toTable", cdl, Boolean.FALSE);

    QueryGenerator qg = new QueryGenerator();
    Assert.assertEquals("insert from one table to another not correct ","INSERT INTO TABLE toDB.toTable SELECT col1, col2, col3, col4, col5 FROM fromDB.fromTable;",qg.generateInsertFromQuery(ifqi));
  }

  @Test
  public void testInsertWithUnhexFromQuery() {
    List<ColumnDescriptionImpl> cdl = new ArrayList<>(4);
    cdl.add(new ColumnDescriptionImpl("col1", ColumnDescription.DataTypes.CHAR.toString(), 0, 10));
    cdl.add(new ColumnDescriptionImpl("col2", ColumnDescription.DataTypes.STRING.toString(), 1));
    cdl.add(new ColumnDescriptionImpl("col3", ColumnDescription.DataTypes.DECIMAL.toString(), 2, 10, 5));
    cdl.add(new ColumnDescriptionImpl("col4", ColumnDescription.DataTypes.VARCHAR.toString(), 3, 40));
    cdl.add(new ColumnDescriptionImpl("col5", ColumnDescription.DataTypes.INT.toString(), 4));

    InsertFromQueryInput ifqi = new InsertFromQueryInput("fromDB","fromTable","toDB","toTable", cdl, Boolean.TRUE);

    QueryGenerator qg = new QueryGenerator();
    Assert.assertEquals("insert from one table to another not correct ","INSERT INTO TABLE toDB.toTable SELECT UNHEX(col1), UNHEX(col2), col3, UNHEX(col4), col5 FROM fromDB.fromTable;",qg.generateInsertFromQuery(ifqi));
  }

  @Test
  public void testDropTableQuery() {

    DeleteQueryInput deleteQueryInput = new DeleteQueryInput("dbName","tableName");

    QueryGenerator qg = new QueryGenerator();
    Assert.assertEquals("drop table query not correct ","DROP TABLE dbName.tableName;",qg.generateDropTableQuery(deleteQueryInput ));
  }
}
