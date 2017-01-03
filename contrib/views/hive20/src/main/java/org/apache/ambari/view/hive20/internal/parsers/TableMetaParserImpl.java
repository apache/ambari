/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.internal.parsers;

import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.internal.dto.DetailedTableInfo;
import org.apache.ambari.view.hive20.internal.dto.PartitionInfo;
import org.apache.ambari.view.hive20.internal.dto.StorageInfo;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;
import org.apache.ambari.view.hive20.internal.dto.ViewInfo;

import javax.inject.Inject;
import java.util.List;

/**
 *
 */
public class TableMetaParserImpl implements TableMetaParser<TableMeta> {

  @Inject
  private CreateTableStatementParser createTableStatementParser;

  @Inject
  private ColumnInfoParser columnInfoParser;

  @Inject
  private PartitionInfoParser partitionInfoParser;

  @Inject
  private DetailedTableInfoParser detailedTableInfoParser;

  @Inject
  private StorageInfoParser storageInfoParser;

  @Inject
  private ViewInfoParser viewInfoParser;



  @Override
  public TableMeta parse(String database, String table, List<Row> createTableStatementRows, List<Row> describeFormattedRows) {
    String createTableStatement = createTableStatementParser.parse(createTableStatementRows);
    DetailedTableInfo tableInfo = detailedTableInfoParser.parse(describeFormattedRows);
    StorageInfo storageInfo = storageInfoParser.parse(describeFormattedRows);
    List<ColumnInfo> columns = columnInfoParser.parse(describeFormattedRows);
    PartitionInfo partitionInfo = partitionInfoParser.parse(describeFormattedRows);
    ViewInfo viewInfo = viewInfoParser.parse(describeFormattedRows);


    TableMeta meta = new TableMeta();
    meta.setId(database + "/" + table);
    meta.setDatabase(database);
    meta.setTable(table);
    meta.setColumns(columns);
    meta.setDdl(createTableStatement);
    meta.setPartitionInfo(partitionInfo);
    meta.setDetailedInfo(tableInfo);
    meta.setStorageInfo(storageInfo);
    meta.setViewInfo(viewInfo);
    return meta;
  }
}
