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

package org.apache.ambari.view.hive20.internal.query.generators;

import com.google.common.base.Optional;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AlterTableQueryGeneratorTest {
  @Test
  public void getQuery() throws Exception {

  }

  @Test
  public void generateColumnQuery() throws Exception {

  }

  @Test
  public void createColumnQueriesForSuccessfulChangeColumn() throws Exception {
    ColumnInfo colInfo1 = new ColumnInfo("col1", "CHAR(1)", "COMMENT 1"); // with comment
    ColumnInfo colInfo2 = new ColumnInfo("col2", "DECIMAL(10,5)"); // no comment
    ColumnInfo colInfo3 = new ColumnInfo("col3", "STRING", "COMMENT-3");
    ColumnInfo colInfo4 = new ColumnInfo("col4", "VARCHAR(10)", "COMMENT 4");
    ColumnInfo colInfo5 = new ColumnInfo("col5", "STRING", "COMMENT 5");
    ColumnInfo colInfo6 = new ColumnInfo("col6", "INT");
    List<ColumnInfo> oldColumns = Arrays.asList(colInfo1, colInfo2, colInfo3);
    List<ColumnInfo> newColumns = Arrays.asList(colInfo4, colInfo5, colInfo6); // all changed
    Optional<List<String>> query = AlterTableQueryGenerator.createColumnQueries(oldColumns, newColumns, false);

    Assert.assertTrue(query.isPresent());
    List<String> queries = query.get();

    Assert.assertEquals("Expected number of column update queries were different.", 3, queries.size());
    String[] expectedQueries = new String[]{" CHANGE COLUMN `col1` `col4` VARCHAR(10) COMMENT \'COMMENT 4\'", " CHANGE COLUMN `col2` `col5` STRING COMMENT \'COMMENT 5\'", " CHANGE COLUMN `col3` `col6` INT"};

    Assert.assertArrayEquals("Column change queries were not equal ", expectedQueries, queries.toArray());
  }

  @Test
  public void createColumnQueriesForSuccessfulChangeAndAddColumn() throws Exception {

    TableMeta oldMeta = new TableMeta();
    TableMeta newMeta = new TableMeta();

    ColumnInfo colInfo1 = new ColumnInfo("col1", "CHAR(1)", "COMMENT 1"); // with comment
    ColumnInfo colInfo2 = new ColumnInfo("col2", "DECIMAL(10,5)"); // no comment
    ColumnInfo colInfo3 = new ColumnInfo("col3", "STRING", "COMMENT-3");
    ColumnInfo colInfo4 = new ColumnInfo("col4", "VARCHAR(10)", "COMMENT 4");
    ColumnInfo colInfo5 = new ColumnInfo("col5", "STRING", "COMMENT 5");
    ColumnInfo colInfo6 = new ColumnInfo("col6", "INT");
    ColumnInfo colInfo7 = new ColumnInfo("col7", "DATE");
    ColumnInfo colInfo8 = new ColumnInfo("col8", "BOOLEAN", "COMMENT 8");

    List<ColumnInfo> oldColumns = Arrays.asList(colInfo1, colInfo2, colInfo3);
    oldMeta.setColumns(oldColumns);

    List<ColumnInfo> newColumns = Arrays.asList(colInfo4, colInfo5, colInfo6, colInfo7, colInfo8); // all changed
    oldMeta.setColumns(newColumns);

    Optional<List<String>> query = AlterTableQueryGenerator.createColumnQueries(oldColumns, newColumns, false);

    Assert.assertTrue(query.isPresent());
    List<String> queries = query.get();

    Assert.assertEquals("Expected number of column update queries were different.", 4, queries.size());
    System.out.println(queries);
    String[] expectedQueries = new String[]{" CHANGE COLUMN `col1` `col4` VARCHAR(10) COMMENT \'COMMENT 4\'", " CHANGE COLUMN `col2` `col5` STRING COMMENT \'COMMENT 5\'", " CHANGE COLUMN `col3` `col6` INT"," ADD COLUMNS ( `col7` DATE, `col8` BOOLEAN COMMENT \'COMMENT 8\' )" };

    Assert.assertArrayEquals("Column change queries were not equal ", expectedQueries, queries.toArray());
  }
}