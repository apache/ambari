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
import com.google.gson.Gson;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class AlterTableQueryGeneratorTest {
  private static final Logger LOG = LoggerFactory.getLogger(AlterTableQueryGeneratorTest.class);

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
    String[] expectedQueries = new String[]{" CHANGE COLUMN `col1` `col4` VARCHAR(10) COMMENT \'COMMENT 4\'", " CHANGE COLUMN `col2` `col5` STRING COMMENT \'COMMENT 5\'", " CHANGE COLUMN `col3` `col6` INT"," ADD COLUMNS ( `col7` DATE, `col8` BOOLEAN COMMENT \'COMMENT 8\' )" };

    Assert.assertArrayEquals("Column change queries were not equal ", expectedQueries, queries.toArray());
  }

  @Test
  public void createColumnQueriesForSuccessfulChangeSomeColumns() throws Exception {

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

    List<ColumnInfo> newColumns = Arrays.asList(colInfo1, colInfo5, colInfo6); // all changed
    oldMeta.setColumns(newColumns);

    Optional<List<String>> query = AlterTableQueryGenerator.createColumnQueries(oldColumns, newColumns, false);

    Assert.assertTrue(query.isPresent());
    List<String> queries = query.get();

    Assert.assertEquals("Expected number of column update queries were different.", 2, queries.size());
    String[] expectedQueries = new String[]{" CHANGE COLUMN `col2` `col5` STRING COMMENT 'COMMENT 5'", " CHANGE COLUMN `col3` `col6` INT"};

    Assert.assertArrayEquals("Column change queries were not equal ", expectedQueries, queries.toArray());
  }

  @Test
  public void createColumnQueriesForSuccessfulAddColumns() throws Exception {

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

    List<ColumnInfo> newColumns = Arrays.asList(colInfo1, colInfo2, colInfo3, colInfo5, colInfo6); // all changed
    oldMeta.setColumns(newColumns);

    Optional<List<String>> query = AlterTableQueryGenerator.createColumnQueries(oldColumns, newColumns, false);

    Assert.assertTrue(query.isPresent());
    List<String> queries = query.get();

    Assert.assertEquals("Expected number of column update queries were different.", 1, queries.size());
    String[] expectedQueries = new String[]{" ADD COLUMNS ( `col5` STRING COMMENT 'COMMENT 5', `col6` INT )"};

    Assert.assertArrayEquals("Column change queries were not equal ", expectedQueries, queries.toArray());
  }

  @Test
  public void getQueryWithAlterColumn(){
    String origMetaString = "{  " +
        "  \"database\": \"default\",  " +
        "  \"table\": \"table2\",  " +
        "  \"columns\": [{  " +
        "   \"name\": \"COL1\",  " +
        "   \"type\": \"TINYINT\",  " +
        "   \"comment\": \"\",  " +
        "   \"precision\": null,  " +
        "   \"scale\": null  " +
        "  }, {  " +
        "   \"name\": \"col2\",  " +
        "   \"type\": \"VARCHAR\",  " +
        "   \"comment\": \"\",  " +
        "   \"precision\": \"333\",  " +
        "   \"scale\": null  " +
        "  }, {  " +
        "   \"name\": \"col3\",  " +
        "   \"type\": \"DECIMAL\",  " +
        "   \"comment\": \"\",  " +
        "   \"precision\": \"33\",  " +
        "   \"scale\": \"3\"  " +
        "  }],  " +
        "  \"partitionInfo\": {  " +
        "   \"columns\": []  " +
        "  },  " +
        "  \"detailedInfo\": {  " +
        "   \"parameters\": {}  " +
        "  },  " +
        "  \"storageInfo\": {}  " +
        " }";
    
    String newMetaString = "{  " +
        "    \"database\": \"default\",  " +
        "    \"table\": \"table2\",  " +
        "    \"columns\": [{  " +
        "      \"name\": \"col1\",  " +
        "      \"type\": \"TINYINT\",  " +
        "      \"comment\": \"\",  " +
        "      \"precision\": null,  " +
        "      \"scale\": null  " +
        "    }, {  " +
        "      \"name\": \"col3\",  " +
        "      \"type\": \"STRING\",  " +
        "      \"comment\": \"\",  " +
        "      \"precision\": \"333\",  " +
        "      \"scale\": null  " +
        "    }, {  " +
        "      \"name\": \"col4\",  " +
        "      \"type\": \"TINYINT\",  " +
        "      \"comment\": \"\",  " +
        "      \"precision\": null,  " +
        "      \"scale\": null  " +
        "    }],  " +
        "    \"partitionInfo\": {  " +
        "      \"columns\": []  " +
        "    },  " +
        "    \"detailedInfo\": {  " +
        "      \"parameters\": {}  " +
        "    },  " +
        "    \"storageInfo\": {}  " +
        "  }";

    Gson gson = new Gson();
    TableMeta origTableMeta = gson.fromJson(origMetaString, TableMeta.class);
    TableMeta updatedTableMeta = gson.fromJson(newMetaString, TableMeta.class);

    LOG.info("origTableMeta : {},\n\nupdatedTableMeta : {}", origMetaString, updatedTableMeta);

    AlterTableQueryGenerator generator = new AlterTableQueryGenerator(origTableMeta, updatedTableMeta);

    Optional<String> query = generator.getQuery();
    Assert.assertTrue(query.isPresent());
    String hqlQuery = query.get();

    LOG.info("hqlQuery : {}", hqlQuery);

    String expectedQuery = " ALTER TABLE `default`.`table2`  CHANGE COLUMN `col2` `col3` STRING(333);\n" +
        " ALTER TABLE `default`.`table2`  CHANGE COLUMN `col3` `col4` TINYINT";
    Assert.assertEquals("Alter Edit table query did not match ", expectedQuery, hqlQuery);
  }
}