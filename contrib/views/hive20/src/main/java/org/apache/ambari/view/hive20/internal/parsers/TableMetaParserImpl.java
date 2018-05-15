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

import org.apache.ambari.view.hive20.client.DatabaseMetadataWrapper;
import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.internal.dto.DetailedTableInfo;
import org.apache.ambari.view.hive20.internal.dto.PartitionInfo;
import org.apache.ambari.view.hive20.internal.dto.StorageInfo;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;
import org.apache.ambari.view.hive20.internal.dto.TableStats;
import org.apache.ambari.view.hive20.internal.dto.ViewInfo;
import org.apache.parquet.Strings;

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
  public TableMeta parse(String database, String table, List<Row> createTableStatementRows, List<Row> describeFormattedRows, DatabaseMetadataWrapper databaseMetadata) {
    String createTableStatement = createTableStatementParser.parse(createTableStatementRows);
    DetailedTableInfo tableInfo = detailedTableInfoParser.parse(describeFormattedRows);
    TableStats tableStats = getTableStats(tableInfo);
    tableStats.setDatabaseMetadata(databaseMetadata);
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
    meta.setTableStats(tableStats);
    return meta;
  }

  private TableStats getTableStats(DetailedTableInfo tableInfo) {
    TableStats tableStats = new TableStats();
    tableStats.setTableStatsEnabled(false);

    String numFiles = tableInfo.getParameters().get(TableStats.NUM_FILES);
    tableInfo.getParameters().remove(TableStats.NUM_FILES);

    String numRows = tableInfo.getParameters().get(TableStats.NUM_ROWS);
    tableInfo.getParameters().remove(TableStats.NUM_ROWS);

    String columnStatsAccurate = tableInfo.getParameters().get(TableStats.COLUMN_STATS_ACCURATE);
    tableInfo.getParameters().remove(TableStats.COLUMN_STATS_ACCURATE);

    String rawDataSize = tableInfo.getParameters().get(TableStats.RAW_DATA_SIZE);
    tableInfo.getParameters().remove(TableStats.RAW_DATA_SIZE);

    String totalSize = tableInfo.getParameters().get(TableStats.TOTAL_SIZE);
    tableInfo.getParameters().remove(TableStats.TOTAL_SIZE);

    if(!Strings.isNullOrEmpty(numFiles) && !Strings.isNullOrEmpty(numFiles.trim())){
      tableStats.setTableStatsEnabled(true);
      tableStats.setNumFiles(Long.valueOf(numFiles.trim()));
    }

    if(!Strings.isNullOrEmpty(numRows) && !Strings.isNullOrEmpty(numRows.trim())){
      tableStats.setTableStatsEnabled(true);
      tableStats.setNumRows(Long.valueOf(numRows.trim()));
    }

    if(!Strings.isNullOrEmpty(rawDataSize) && !Strings.isNullOrEmpty(rawDataSize.trim())){
      tableStats.setTableStatsEnabled(true);
      tableStats.setRawDataSize(Long.valueOf(rawDataSize.trim()));
    }

    if(!Strings.isNullOrEmpty(totalSize) && !Strings.isNullOrEmpty(totalSize.trim())){
      tableStats.setTableStatsEnabled(true);
      tableStats.setTotalSize(Long.valueOf(totalSize.trim()));
    }

    if(!Strings.isNullOrEmpty(columnStatsAccurate) && !Strings.isNullOrEmpty(columnStatsAccurate.trim())) {
      tableStats.setTableStatsEnabled(true);
      tableStats.setColumnStatsAccurate(columnStatsAccurate);
    }
    return tableStats;
  }
}
